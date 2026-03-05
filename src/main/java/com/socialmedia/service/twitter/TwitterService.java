package com.socialmedia.service.twitter;

import com.socialmedia.client.twitter.TwitterApiClient;
import com.socialmedia.client.twitter.response.TwitterTweetResponse;
import com.socialmedia.client.twitter.response.TwitterUserResponse;
import com.socialmedia.client.twitter.response.UserAboutResponse;
import com.socialmedia.dto.response.Tweet;
import com.socialmedia.dto.response.SearchResult;
import com.socialmedia.entity.User;
import com.socialmedia.exception.TwitterApiException;
import com.socialmedia.service.SocialMediaService;
import com.socialmedia.service.UserService;
import com.socialmedia.service.TweetService;
import com.socialmedia.service.mapper.TweetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TwitterService implements SocialMediaService {

    private static final Logger logger = LoggerFactory.getLogger(TwitterService.class);

    private final TwitterApiClient twitterApiClient;
    private final UserService userService;
    private final TweetService tweetService;
    private final TweetMapper tweetMapper;

    public TwitterService(TwitterApiClient twitterApiClient, 
                         UserService userService,
                         TweetService tweetService,
                         TweetMapper tweetMapper) {
        this.twitterApiClient = twitterApiClient;
        this.userService = userService;
        this.tweetService = tweetService;
        this.tweetMapper = tweetMapper;
    }

    /**
     * Fetches user_about from Twitter API and sets about_profile on the user.
     * Logs and returns without throwing if the API call fails.
     */
    private void enrichUserWithAboutProfile(User user) {
        if (user == null || user.getUserName() == null || user.getUserName().isBlank()) {
            return;
        }
        try {
            UserAboutResponse aboutResponse = twitterApiClient.getUserAbout(user.getUserName());
            if (aboutResponse != null) {
                Map<String, Object> aboutProfile = aboutResponse.getAboutProfile();
                if (aboutProfile != null && !aboutProfile.isEmpty()) {
                    user.setAboutProfile(aboutProfile);
                    logger.debug("Enriched user {} with about_profile", user.getUserName());
                }
            }
        } catch (Exception e) {
            logger.debug("Could not fetch user_about for {}: {}", user.getUserName(), e.getMessage());
        }
    }

    /**
     * Build a User from get_user_about API "data" map (id, name, userName, createdAt, isBlueVerified, about_profile).
     */
    private User userFromAboutResponse(String requestedUsername, com.socialmedia.client.twitter.response.UserAboutResponse aboutResp) {
        Map<String, Object> data = aboutResp.getData();
        if (data == null) return null;
        Object userNameObj = data.get("userName");
        String userName = userNameObj != null ? userNameObj.toString().trim() : requestedUsername.trim();
        if (userName.isEmpty()) return null;

        User user = new User();
        user.setPlatformName("twitter");
        user.setUserName(userName);
        Object idObj = data.get("id");
        if (idObj != null) user.setPlatformId(idObj.toString());
        Object nameObj = data.get("name");
        if (nameObj != null) user.setName(nameObj.toString());
        Object createdAtObj = data.get("createdAt");
        if (createdAtObj != null) {
            try {
                user.setCreatedAt(Instant.parse(createdAtObj.toString()));
            } catch (Exception ignored) {}
        }
        Object isBlueObj = data.get("isBlueVerified");
        if (isBlueObj instanceof Boolean) user.setIsBlueVerified((Boolean) isBlueObj);
        user.setAboutProfile(aboutResp.getAboutProfile());
        user.setTweets(null);
        return user;
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userService.findByUsername(username);
    }

    @Override
    public Optional<User> getUserPreview(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        String q = username.trim();
        Optional<User> fromDb = userService.findByUsername(q);
        if (fromDb.isPresent()) return fromDb;
        return fetchUserFromApiWithoutSaving(q);
    }

    @Override
    @Transactional
    public Optional<User> saveUserByUsername(String username) {
        return searchUsernames(username != null ? username.trim() : "");
    }

    /**
     * Fetches user from Twitter API (getUserProfile, searchUsers, getUserAbout) without saving.
     */
    private Optional<User> fetchUserFromApiWithoutSaving(String searchString) {
        try {
            TwitterUserResponse profileResponse = twitterApiClient.getUserProfile(searchString);
            if (profileResponse != null && profileResponse.getData() != null) {
                User user = profileResponse.getData();
                if (user.getUserName() != null && !user.getUserName().trim().isEmpty()) {
                    user.setUserName(user.getUserName().trim());
                    user.setPlatformName("twitter");
                    user.setTweets(null);
                    enrichUserWithAboutProfile(user);
                    return Optional.of(user);
                }
            }
        } catch (Exception e) {
            logger.debug("getUserProfile failed for preview {}, trying search: {}", searchString, e.getMessage());
        }
        TwitterUserResponse response = twitterApiClient.searchUsers(searchString);
        List<User> usersList = response != null ? response.getUsersList() : Collections.emptyList();
        if (usersList.isEmpty()) {
            try {
                com.socialmedia.client.twitter.response.UserAboutResponse aboutResp = twitterApiClient.getUserAbout(searchString);
                if (aboutResp != null && "success".equalsIgnoreCase(aboutResp.getStatus()) && aboutResp.getData() != null) {
                    User u = userFromAboutResponse(searchString, aboutResp);
                    if (u != null) {
                        enrichUserWithAboutProfile(u);
                        return Optional.of(u);
                    }
                }
            } catch (Exception e) {
                logger.debug("get_user_about fallback failed for {}: {}", searchString, e.getMessage());
            }
            return Optional.empty();
        }
        return usersList.stream()
                .filter(user -> user.getUserName() != null && !user.getUserName().trim().isEmpty())
                .map(user -> {
                    user.setUserName(user.getUserName().trim());
                    user.setPlatformName("twitter");
                    user.setTweets(null);
                    enrichUserWithAboutProfile(user);
                    return user;
                })
                .findFirst();
    }

    @Override
    @Transactional
    public Optional<User> searchUsernames(String searchString) {
        // Try to get user profile first (more accurate for exact username match)
        // If that fails, fall back to search
        try {
            TwitterUserResponse profileResponse = twitterApiClient.getUserProfile(searchString);
            if (profileResponse != null && profileResponse.getData() != null) {
                User user = profileResponse.getData();
                
                // Debug: log what we received from API
                logger.info("User Profile from API - userName={}, name={}, followers={}, following={}, favouritesCount={}, statusesCount={}, mediaCount={}", 
                        user.getUserName(), user.getName(), 
                        user.getFollowers(), user.getFollowing(), 
                        user.getFavouritesCount(), user.getStatusesCount(), 
                        user.getMediaCount());
                
                // Validate userName
                if (user.getUserName() != null && !user.getUserName().trim().isEmpty()) {
                    // Trim userName
                    user.setUserName(user.getUserName().trim());
                    
                    // Set platform information
                    user.setPlatformName("twitter");
                    
                    // Clear lazy-loaded collections to avoid serialization issues
                    user.setTweets(null);

                    enrichUserWithAboutProfile(user);
                    
                    // Log before saving
                    logger.info("User before saving - userName={}, followers={}, following={}, favouritesCount={}, statusesCount={}, mediaCount={}", 
                            user.getUserName(), user.getFollowers(), user.getFollowing(), 
                            user.getFavouritesCount(), user.getStatusesCount(), user.getMediaCount());
                    
                    // Save to database
                    User savedUser = userService.saveOrUpdateUserByUsername(user);
                    
                    // Log after saving
                    logger.info("User after saving - userName={}, followers={}, following={}, favouritesCount={}, statusesCount={}, mediaCount={}", 
                            savedUser.getUserName(), savedUser.getFollowers(), savedUser.getFollowing(), 
                            savedUser.getFavouritesCount(), savedUser.getStatusesCount(), savedUser.getMediaCount());
                    
                    return Optional.of(savedUser);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get user profile, falling back to search: {}", e.getMessage());
        }
        
        // Fall back to search if profile endpoint fails
        TwitterUserResponse response = twitterApiClient.searchUsers(searchString);
        
        // Use getUsersList() to handle both users array and data field formats
        List<User> usersList = response != null ? response.getUsersList() : Collections.emptyList();
        
        if (usersList.isEmpty()) {
            // Third fallback: get_user_about returns user identity (id, name, userName, etc.)
            try {
                com.socialmedia.client.twitter.response.UserAboutResponse aboutResp = twitterApiClient.getUserAbout(searchString);
                if (aboutResp != null && "success".equalsIgnoreCase(aboutResp.getStatus()) && aboutResp.getData() != null) {
                    User userFromAbout = userFromAboutResponse(searchString, aboutResp);
                    if (userFromAbout != null) {
                        enrichUserWithAboutProfile(userFromAbout);
                        User saved = userService.saveOrUpdateUserByUsername(userFromAbout);
                        return Optional.of(saved);
                    }
                }
            } catch (Exception e) {
                logger.debug("get_user_about fallback failed for {}: {}", searchString, e.getMessage());
            }
            return Optional.empty();
        }

        // Find the first valid user and save it
        return usersList.stream()
                .map(user -> {
                    // Debug: log what we received from API
                    logger.info("User after API parsing - userName={}, name={}, followers={}, following={}, favouritesCount={}, statusesCount={}, mediaCount={}", 
                            user.getUserName(), user.getName(), 
                            user.getFollowers(), user.getFollowing(), 
                            user.getFavouritesCount(), user.getStatusesCount(), 
                            user.getMediaCount());
                    
                    // Validate userName
                    if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
                        logger.warn("Skipping user - userName is null or empty. name={}, id={}, url={}", 
                                user.getName(), user.getId(), user.getUrl());
                        return null;
                    }
                    
                    // Trim userName
                    user.setUserName(user.getUserName().trim());
                    
                    // Set platform information
                    user.setPlatformName("twitter");
                    
                    // Clear lazy-loaded collections to avoid serialization issues
                    user.setTweets(null);

                    enrichUserWithAboutProfile(user);
                    
                    // Log before saving
                    logger.info("User before saving - userName={}, followers={}, following={}, favouritesCount={}, statusesCount={}, mediaCount={}", 
                            user.getUserName(), user.getFollowers(), user.getFollowing(), 
                            user.getFavouritesCount(), user.getStatusesCount(), user.getMediaCount());
                    
                    // Save to database
                    User savedUser = userService.saveOrUpdateUserByUsername(user);
                    
                    // Log after saving
                    logger.info("User after saving - userName={}, followers={}, following={}, favouritesCount={}, statusesCount={}, mediaCount={}", 
                            savedUser.getUserName(), savedUser.getFollowers(), savedUser.getFollowing(), 
                            savedUser.getFavouritesCount(), savedUser.getStatusesCount(), savedUser.getMediaCount());
                    
                    return savedUser;
                })
                .filter(user -> user != null)
                .findFirst();
    }

    @Override
    @Transactional
    public List<Tweet> getTweets(String username, Instant startTime, Instant endTime) {
        // First, check if user exists in database
        Optional<User> existingUser = userService.findByUsername(username);
        User author;
        
        if (existingUser.isPresent()) {
            author = existingUser.get();
            // Try to get tweets from database first
            List<com.socialmedia.entity.Tweet> dbTweets = tweetService.getTweetsByAuthorAndTimeRange(author, startTime, endTime);
            
            // If we have tweets in DB, return them
            if (!dbTweets.isEmpty()) {
                return dbTweets.stream()
                        .map(this::toDto)
                        .collect(Collectors.toList());
            }
        } else {
            // User not in database - try user/info first, then search, then get_user_about
            User userToSave = null;
            try {
                TwitterUserResponse profileResponse = twitterApiClient.getUserProfile(username);
                if (profileResponse != null && profileResponse.getData() != null) {
                    userToSave = profileResponse.getData();
                }
            } catch (Exception e) {
                logger.debug("getUserProfile failed for {}, trying search: {}", username, e.getMessage());
            }
            if (userToSave == null) {
                TwitterUserResponse userResponse = twitterApiClient.searchUsers(username);
                List<User> usersList = userResponse != null ? userResponse.getUsersList() : Collections.emptyList();
                Optional<User> foundUser = usersList.stream()
                        .filter(user -> user.getUserName() != null && user.getUserName().trim().equalsIgnoreCase(username.trim()))
                        .findFirst();
                if (foundUser.isEmpty()) {
                    foundUser = usersList.stream()
                            .filter(user -> user.getUserName() != null && !user.getUserName().trim().isEmpty())
                            .findFirst();
                }
                if (foundUser.isPresent()) userToSave = foundUser.get();
            }
            if (userToSave == null) {
                try {
                    com.socialmedia.client.twitter.response.UserAboutResponse aboutResp = twitterApiClient.getUserAbout(username);
                    if (aboutResp != null && "success".equalsIgnoreCase(aboutResp.getStatus())) {
                        userToSave = userFromAboutResponse(username, aboutResp);
                    }
                } catch (Exception e) {
                    logger.debug("get_user_about failed for {}: {}", username, e.getMessage());
                }
            }
            if (userToSave == null) {
                throw new IllegalArgumentException("User not found: " + username);
            }
            userToSave.setUserName(userToSave.getUserName().trim());
            userToSave.setPlatformName("twitter");
            userToSave.setTweets(null);
            enrichUserWithAboutProfile(userToSave);
            author = userService.saveOrUpdateUserByUsername(userToSave);
        }
        
        // Now fetch tweets from Twitter API using the username
        TwitterTweetResponse response = twitterApiClient.getTweets(username, startTime, endTime);
        
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return Collections.emptyList();
        }

        // Convert and save tweets
        List<com.socialmedia.entity.Tweet> tweets = response.getData().stream()
                .map(twitterTweet -> tweetMapper.toEntity(twitterTweet, author))
                .filter(tweet -> tweet != null)
                .collect(Collectors.toList());
        
        List<com.socialmedia.entity.Tweet> savedTweets = tweetService.saveTweets(tweets);
        
        // Convert to DTOs
        return savedTweets.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<User> searchUsersByKeyword(String keyword) {
        List<User> result = new ArrayList<>();
        String q = keyword != null ? keyword.trim() : "";
        boolean looksLikeHandle = !q.isEmpty() && !q.contains(" ") && q.length() <= 50;

        // If keyword looks like a single handle, try get user/info first (exact lookup).
        // Do not save to DB; return for display only. User is saved only when they click Save on the details page.
        if (looksLikeHandle) {
            try {
                TwitterUserResponse profileResponse = twitterApiClient.getUserProfile(q);
                if (profileResponse != null && profileResponse.getData() != null) {
                    User user = profileResponse.getData();
                    if (user.getUserName() != null && !user.getUserName().trim().isEmpty()) {
                        user.setUserName(user.getUserName().trim());
                        user.setPlatformName("twitter");
                        user.setTweets(null);
                        enrichUserWithAboutProfile(user);
                        result.add(user);
                    }
                }
            } catch (Exception e) {
                logger.debug("User info lookup for handle \"{}\" failed, continuing with search: {}", q, e.getMessage());
            }
        }

        TwitterUserResponse response = twitterApiClient.searchUsers(keyword);
        List<User> usersList = response != null ? response.getUsersList() : Collections.emptyList();

        for (User user : usersList) {
            if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
                logger.warn("Skipping user - userName is null or empty. name={}, id={}, url={}", 
                        user.getName(), user.getId(), user.getUrl());
                continue;
            }
            String un = user.getUserName().trim();
            // Skip if we already have this user from handle lookup
            if (result.stream().anyMatch(u -> un.equalsIgnoreCase(u.getUserName()))) continue;
            user.setUserName(un);
            user.setPlatformName("twitter");
            user.setTweets(null);
            enrichUserWithAboutProfile(user);
            result.add(user);
        }
        return result;
    }

    /**
     * Asynchronously load all tweets for a user within a date range.
     * This method handles pagination and loads all available tweets.
     */
    @Async("twitterTaskExecutor")
    @Transactional
    public void loadTweetsAsync(String username, Instant startTime, Instant endTime) {
        try {
            // First, ensure user exists in database
            Optional<User> existingUser = userService.findByUsername(username);
            User author;
            
            if (existingUser.isPresent()) {
                author = existingUser.get();
            } else {
                // User not in database - try user/info, then search, then get_user_about
                User userToSave = null;
                try {
                    TwitterUserResponse profileResponse = twitterApiClient.getUserProfile(username);
                    if (profileResponse != null && profileResponse.getData() != null) {
                        userToSave = profileResponse.getData();
                    }
                } catch (Exception e) {
                    logger.debug("getUserProfile failed for {}, trying search: {}", username, e.getMessage());
                }
                if (userToSave == null) {
                    TwitterUserResponse userResponse = twitterApiClient.searchUsers(username);
                    List<User> usersList = userResponse != null ? userResponse.getUsersList() : Collections.emptyList();
                    Optional<User> foundUser = usersList.stream()
                            .filter(user -> user.getUserName() != null && user.getUserName().trim().equalsIgnoreCase(username.trim()))
                            .findFirst();
                    if (foundUser.isEmpty()) {
                        foundUser = usersList.stream()
                                .filter(user -> user.getUserName() != null && !user.getUserName().trim().isEmpty())
                                .findFirst();
                    }
                    if (foundUser.isPresent()) userToSave = foundUser.get();
                }
                if (userToSave == null) {
                    try {
                        com.socialmedia.client.twitter.response.UserAboutResponse aboutResp = twitterApiClient.getUserAbout(username);
                        if (aboutResp != null && "success".equalsIgnoreCase(aboutResp.getStatus())) {
                            userToSave = userFromAboutResponse(username, aboutResp);
                        }
                    } catch (Exception e) {
                        logger.debug("get_user_about failed for {}: {}", username, e.getMessage());
                    }
                }
                if (userToSave == null) {
                    logger.error("User not found: {}", username);
                    return;
                }
                userToSave.setUserName(userToSave.getUserName().trim());
                userToSave.setPlatformName("twitter");
                userToSave.setTweets(null);
                enrichUserWithAboutProfile(userToSave);
                author = userService.saveOrUpdateUserByUsername(userToSave);
            }
            
            // Load all tweets with pagination
            String cursor = null;
            int totalTweetsLoaded = 0;
            int pageCount = 0;
            
            do {
                pageCount++;
                logger.info("Loading tweets page {} for user: {} (cursor: {})", pageCount, username, cursor != null ? cursor.substring(0, Math.min(20, cursor.length())) + "..." : "null");
                
                long startTimeMs = System.currentTimeMillis();
                TwitterTweetResponse response;
                try {
                    response = twitterApiClient.getTweets(username, startTime, endTime, cursor);
                    long duration = System.currentTimeMillis() - startTimeMs;
                    logger.info("API call completed for page {} in {} ms", pageCount, duration);
                } catch (Exception e) {
                    logger.error("API call failed for page {}: {}", pageCount, e.getMessage(), e);
                    break;
                }
                
                if (response == null) {
                    logger.warn("Response is null for page {}, stopping pagination", pageCount);
                    break;
                }
                
                if (response.getData() == null || response.getData().isEmpty()) {
                    logger.info("No tweets returned for page {}, stopping pagination", pageCount);
                    break;
                }
                
                logger.info("Received {} tweets in page {}", response.getData().size(), pageCount);
                
                // Convert and save tweets
                List<com.socialmedia.entity.Tweet> tweets = response.getData().stream()
                        .map(twitterTweet -> {
                            com.socialmedia.entity.Tweet tweet = tweetMapper.toEntity(twitterTweet, author);
                            if (tweet != null) {
                                String textPreview = tweet.getText() != null && tweet.getText().length() > 100 
                                        ? tweet.getText().substring(0, 100) + "..." 
                                        : tweet.getText();
                                logger.info("Processing tweet - ID: {}, Text: {}, CreatedAt: {}, Likes: {}, Retweets: {}", 
                                        tweet.getId(), textPreview, tweet.getCreatedAt(), 
                                        tweet.getLikeCount(), tweet.getRetweetCount());
                            }
                            return tweet;
                        })
                        .filter(tweet -> tweet != null)
                        .collect(Collectors.toList());
                
                List<com.socialmedia.entity.Tweet> savedTweets = tweetService.saveTweets(tweets);
                totalTweetsLoaded += savedTweets.size();
                
                logger.info("Page {}: Saved {} tweets (total loaded: {})", pageCount, savedTweets.size(), totalTweetsLoaded);
                
                // Check if there are more pages
                boolean hasNextPage = response.getHasNextPage() != null && response.getHasNextPage();
                String nextCursor = response.getNextCursor();
                
                logger.info("Page {} pagination info - hasNextPage: {}, nextCursor: {}", 
                        pageCount, hasNextPage, nextCursor != null ? nextCursor.substring(0, Math.min(20, nextCursor.length())) + "..." : "null");
                
                if (hasNextPage && nextCursor != null && !nextCursor.isEmpty()) {
                    cursor = nextCursor;
                    logger.info("Continuing to next page with cursor");
                } else {
                    cursor = null;
                    logger.info("No more pages available, pagination complete");
                }
                
            } while (cursor != null && !cursor.isEmpty());
            
            logger.info("Completed loading tweets for user: {}. Total tweets loaded: {}", username, totalTweetsLoaded);
            
        } catch (Exception e) {
            logger.error("Failed to load tweets asynchronously for user: {}", username, e);
        }
    }

    /**
     * Search tweets by keyword using Twitter advanced search.
     * Note: This method is not transactional as the save operations (saveTweets, saveOrUpdateUserByUsername)
     * are already transactional. This prevents transaction rollback issues when exceptions occur.
     * 
     * @param keyword The keyword to search for
     * @param queryType "Latest" or "Top" (default: "Latest")
     * @param cursor Cursor for pagination (null for first page)
     * @return SearchResult containing tweets and pagination info
     */
    public SearchResult searchTweetsByKeyword(String keyword, String queryType, String cursor) {
        return searchTweetsByKeyword(keyword, queryType, cursor, null, null);
    }

    /**
     * Search tweets by keyword with optional date range (since/until in yyyy-MM-dd).
     * When both since and until are provided, appends " since:YYYY-MM-DD until:YYYY-MM-DD" to the query.
     */
    public SearchResult searchTweetsByKeyword(String keyword, String queryType, String cursor, String since, String until) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return SearchResult.builder()
                    .tweets(Collections.emptyList())
                    .hasNextPage(false)
                    .nextCursor(null)
                    .count(0)
                    .build();
        }
        
        if (queryType == null || queryType.trim().isEmpty()) {
            queryType = "Latest";
        }

        String query = keyword.trim();
        if (since != null && !since.isBlank() && until != null && !until.isBlank()) {
            query = query + " since:" + since.trim() + " until:" + until.trim();
        }
        
        // Convert tweets directly to DTOs without saving to database
        // This avoids transaction issues and makes search faster
        List<Tweet> tweets = new ArrayList<>();
        
        try {
            logger.info("Searching tweets by keyword: '{}' with queryType: {}, cursor: {}, since: {}, until: {}", 
                    keyword, queryType, cursor != null ? cursor.substring(0, Math.min(20, cursor.length())) + "..." : "null", since, until);
            TwitterTweetResponse response = twitterApiClient.advancedSearchTweets(query, queryType, cursor);
            
            if (response == null) {
                logger.warn("Twitter API returned null response for keyword: {}", keyword);
                return SearchResult.builder()
                        .tweets(Collections.emptyList())
                        .hasNextPage(false)
                        .nextCursor(null)
                        .count(0)
                        .build();
            }
            
            if (response.getData() == null || response.getData().isEmpty()) {
                logger.info("No tweets found for keyword: {}", keyword);
                return SearchResult.builder()
                        .tweets(Collections.emptyList())
                        .hasNextPage(false)
                        .nextCursor(null)
                        .count(0)
                        .build();
            }
            
            logger.info("Received {} tweets from API for keyword: {}", response.getData().size(), keyword);
            
            // Convert each tweet to DTO
            for (TwitterTweetResponse.TwitterTweet twitterTweet : response.getData()) {
                try {
                    if (twitterTweet.getAuthor() == null) {
                        continue;
                    }
                    
                    // Convert tweet directly to DTO without saving
                    Tweet dto = Tweet.builder()
                            .id(twitterTweet.getId())
                            .text(twitterTweet.getText())
                            .authorUsername(twitterTweet.getAuthor() != null ? 
                                    twitterTweet.getAuthor().getUserName() : null)
                            .createdAt(twitterTweet.getCreatedAtAsInstant())
                            .likeCount(twitterTweet.getLikeCount() != null ? 
                                    twitterTweet.getLikeCount().longValue() : null)
                            .retweetCount(twitterTweet.getRetweetCount() != null ? 
                                    twitterTweet.getRetweetCount().longValue() : null)
                            .build();
                    
                    tweets.add(dto);
                } catch (Exception e) {
                    // Log error for individual tweet but continue processing others
                    logger.warn("Failed to convert tweet to DTO: {}", e.getMessage());
                    continue;
                }
            }
            
            // Extract pagination info from response
            Boolean hasNextPage = response.getHasNextPage() != null && response.getHasNextPage();
            String nextCursor = response.getNextCursor();
            
            logger.info("Successfully converted {} tweets out of {} received for keyword: {}. Has next page: {}", 
                    tweets.size(), response.getData().size(), keyword, hasNextPage);
            
            return SearchResult.builder()
                    .tweets(tweets)
                    .hasNextPage(hasNextPage)
                    .nextCursor(nextCursor)
                    .count(tweets.size())
                    .build();
        } catch (TwitterApiException e) {
            logger.error("Twitter API error while searching tweets by keyword: {}", keyword, e);
            // Return empty result for API errors
            return SearchResult.builder()
                    .tweets(Collections.emptyList())
                    .hasNextPage(false)
                    .nextCursor(null)
                    .count(0)
                    .build();
        } catch (Exception e) {
            logger.error("Failed to search tweets by keyword: {}", keyword, e);
            // Log the full exception for debugging
            logger.error("Exception details: ", e);
            // Return empty result on error
            return SearchResult.builder()
                    .tweets(Collections.emptyList())
                    .hasNextPage(false)
                    .nextCursor(null)
                    .count(0)
                    .build();
        }
    }

    /**
     * Asynchronously load all tweets matching a keyword using Twitter advanced search.
     * Handles pagination and loads all available tweets. For each tweet, if the author
     * is not in the user database, fetches full user details via user search and saves
     * the tweet with that author.
     *
     * @param keyword The keyword to search for
     * @param queryType "Latest" or "Top" (default: "Latest")
     */
    @Async("twitterTaskExecutor")
    @Transactional
    public void loadTweetsByKeywordAsync(String keyword, String queryType) {
        loadTweetsByKeywordAsyncInternal(keyword != null ? keyword.trim() : "", queryType);
    }

    /**
     * Asynchronously load all tweets matching a keyword within a date range.
     * Builds query with since/until and ensures each tweet's author is in the user DB
     * (fetches via user search when missing), then saves each tweet to the tweet database.
     *
     * @param keyword The keyword to search for
     * @param queryType "Latest" or "Top" (default: "Latest")
     * @param startTime Start of range (included in query as since:)
     * @param endTime End of range (included in query as until:)
     */
    @Async("twitterTaskExecutor")
    @Transactional
    public void loadTweetsByKeywordAsync(String keyword, String queryType, Instant startTime, Instant endTime) {
        String query = keyword != null ? keyword.trim() : "";
        if (startTime != null && endTime != null) {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
            query = query + " since:" + f.format(startTime) + " until:" + f.format(endTime);
        }
        loadTweetsByKeywordAsyncInternal(query, queryType);
    }

    private void loadTweetsByKeywordAsyncInternal(String query, String queryType) {
        if (query == null || query.trim().isEmpty()) {
            logger.error("Query cannot be empty");
            return;
        }
        if (queryType == null || queryType.trim().isEmpty()) {
            queryType = "Latest";
        }
        try {
            logger.info("Starting async keyword search for: {} (queryType: {})", query, queryType);
            String cursor = null;
            int totalTweetsLoaded = 0;
            int pageCount = 0;
            do {
                pageCount++;
                logger.info("Loading tweets page {} for query (cursor: {})",
                        pageCount, cursor != null ? cursor.substring(0, Math.min(20, cursor.length())) + "..." : "null");
                long startMs = System.currentTimeMillis();
                TwitterTweetResponse response;
                try {
                    response = twitterApiClient.advancedSearchTweets(query, queryType, cursor);
                    logger.info("API call completed for page {} in {} ms", pageCount, System.currentTimeMillis() - startMs);
                } catch (Exception e) {
                    logger.error("API call failed for page {}: {}", pageCount, e.getMessage(), e);
                    break;
                }
                if (response == null || response.getData() == null || response.getData().isEmpty()) {
                    if (response == null || response.getData() == null) break;
                    logger.info("No tweets returned for page {}, stopping", pageCount);
                    break;
                }
                logger.info("Received {} tweets in page {}", response.getData().size(), pageCount);
                List<com.socialmedia.entity.Tweet> tweetsToSave = new ArrayList<>();
                for (TwitterTweetResponse.TwitterTweet twitterTweet : response.getData()) {
                    if (twitterTweet.getAuthor() == null) continue;
                    User author = twitterTweet.getAuthor();
                    String authorUserName = author.getUserName() != null ? author.getUserName().trim() : "";
                    if (authorUserName.isEmpty()) {
                        logger.warn("Skipping tweet - author has no userName");
                        continue;
                    }
                    User authorToUse;
                    Optional<User> existing = userService.findByUsername(authorUserName);
                    if (existing.isPresent()) {
                        authorToUse = existing.get();
                    } else {
                        Optional<User> searched = searchUsernames(authorUserName);
                        if (searched.isPresent()) {
                            authorToUse = searched.get();
                        } else {
                            author.setUserName(authorUserName);
                            author.setPlatformName("twitter");
                            author.setTweets(null);
                            authorToUse = userService.saveOrUpdateUserByUsername(author);
                        }
                    }
                    com.socialmedia.entity.Tweet tweet = tweetMapper.toEntity(twitterTweet, authorToUse);
                    if (tweet != null) tweetsToSave.add(tweet);
                }
                List<com.socialmedia.entity.Tweet> savedTweets = tweetService.saveTweets(tweetsToSave);
                totalTweetsLoaded += savedTweets.size();
                logger.info("Page {}: Saved {} tweets (total loaded: {})", pageCount, savedTweets.size(), totalTweetsLoaded);
                boolean hasNext = response.getHasNextPage() != null && response.getHasNextPage();
                String nextCursor = response.getNextCursor();
                if (hasNext && nextCursor != null && !nextCursor.isEmpty()) {
                    cursor = nextCursor;
                } else {
                    cursor = null;
                }
            } while (cursor != null && !cursor.isEmpty());
            logger.info("Completed loading tweets for keyword query. Total tweets loaded: {}", totalTweetsLoaded);
        } catch (Exception e) {
            logger.error("Failed to load tweets asynchronously for keyword query: {}", query, e);
        }
    }

    private Tweet toDto(com.socialmedia.entity.Tweet entity) {
        return Tweet.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .text(entity.getText())
                .authorUsername(entity.getAuthor() != null ? entity.getAuthor().getUserName() : null)
                .createdAt(entity.getCreatedAt())
                .likeCount(entity.getLikeCount() != null ? entity.getLikeCount().longValue() : null)
                .retweetCount(entity.getRetweetCount() != null ? entity.getRetweetCount().longValue() : null)
                .build();
    }
}
