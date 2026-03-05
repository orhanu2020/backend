package com.socialmedia.client.twitter;

import com.socialmedia.client.twitter.response.TwitterTweetResponse;
import com.socialmedia.client.twitter.response.TwitterUserResponse;
import com.socialmedia.client.twitter.response.UserAboutResponse;
import com.socialmedia.exception.TwitterApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class TwitterApiClient {

    private static final Logger logger = LoggerFactory.getLogger(TwitterApiClient.class);

    /** Full URL for user info by screen name: https://api.twitterapi.io/twitter/user/info */
    private static final String USER_INFO_URL = "https://api.twitterapi.io/twitter/user/info";
    /** Full URL for user about: https://api.twitterapi.io/twitter/user_about */
    private static final String USER_ABOUT_URL = "https://api.twitterapi.io/twitter/user_about";

    private final WebClient webClient;
    private final String apiKey;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    
    /**
     * Creates a properly configured ObjectMapper with JSR310 module for Instant support
     */
    private static com.fasterxml.jackson.databind.ObjectMapper createObjectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Disable builder-based deserialization to use setters instead
        mapper.configure(com.fasterxml.jackson.databind.MapperFeature.USE_GETTERS_AS_SETTERS, false);
        return mapper;
    }

    public TwitterApiClient(WebClient webClient, @Value("${twitter.api.key}") String apiKey) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    public TwitterUserResponse searchUsers(String searchString) {
        try {
            logger.debug("Searching users with query: {}", searchString);
            long startTime = System.currentTimeMillis();
            
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/twitter/user/search")
                            .queryParam("query", searchString)
                            .build())
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(120))
                    .retry(2) // Retry up to 2 times on failure
                    .doOnSubscribe(sub -> logger.debug("Starting user search API call"))
                    .doOnSuccess(result -> logger.debug("User search API call completed in {} ms", System.currentTimeMillis() - startTime))
                    .doOnError(error -> logger.warn("User search API call failed after {} ms: {}", System.currentTimeMillis() - startTime, error.getMessage()))
                    .block();
            
            // Log raw response for debugging
            if (responseBody != null && responseBody.length() < 10000) {
                logger.info("Twitter API Raw Response (first 10000 chars): {}", 
                        responseBody.substring(0, Math.min(10000, responseBody.length())));
            }
            
            // Parse JSON manually to handle potential field name issues
            com.fasterxml.jackson.databind.ObjectMapper mapper = createObjectMapper();
            TwitterUserResponse response = mapper.readValue(responseBody, TwitterUserResponse.class);
            
            // Log parsed response structure
            if (response != null && response.getData() != null) {
                com.socialmedia.entity.User user = response.getData();
                logger.info("Parsed User from API - followers={}, following={}, favouritesCount={}, statusesCount={}, mediaCount={}", 
                        user.getFollowers(), user.getFollowing(), 
                        user.getFavouritesCount(), user.getStatusesCount(), 
                        user.getMediaCount());
            } else if (response != null && response.getUsers() != null && !response.getUsers().isEmpty()) {
                com.socialmedia.entity.User user = response.getUsers().get(0);
                logger.info("Parsed User from API (users array) - followers={}, following={}, favouritesCount={}, statusesCount={}, mediaCount={}", 
                        user.getFollowers(), user.getFollowing(), 
                        user.getFavouritesCount(), user.getStatusesCount(), 
                        user.getMediaCount());
            }
            
            // Debug: log parsed response
            if (response != null && response.getUsers() != null && !response.getUsers().isEmpty()) {
                logger.debug("Parsed first user - userName={}, name={}", 
                        response.getUsers().get(0).getUserName(), response.getUsers().get(0).getName());
            }
            
            return response;
        } catch (WebClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            String responseBody = e.getResponseBodyAsString();
            logger.error("Twitter API Error Response: {}", responseBody);
            throw new TwitterApiException("Failed to search users: " + e.getMessage(), status);
        } catch (Exception e) {
            logger.error("Exception while searching users: {}", e.getMessage(), e);
            throw new TwitterApiException("Unexpected error while searching users: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get user info by screen name.
     * Calls https://api.twitterapi.io/twitter/user/info
     * @see <a href="https://docs.twitterapi.io/api-reference/endpoint/get_user_by_username">Get User Info</a>
     */
    public TwitterUserResponse getUserProfile(String username) {
        try {
            logger.debug("Getting user info for: {}", username);
            long startTime = System.currentTimeMillis();

            String encodedUserName = URLEncoder.encode(username, StandardCharsets.UTF_8);
            URI uri = URI.create(USER_INFO_URL + "?userName=" + encodedUserName);

            String responseBody = webClient.get()
                    .uri(uri)
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(120))
                    .retry(2)
                    .doOnSubscribe(sub -> logger.debug("Starting user info API call"))
                    .doOnSuccess(result -> logger.debug("User info API call completed in {} ms", System.currentTimeMillis() - startTime))
                    .doOnError(error -> logger.warn("User info API call failed after {} ms: {}", System.currentTimeMillis() - startTime, error.getMessage()))
                    .block();

            // Log raw response for debugging
            if (responseBody != null && responseBody.length() < 10000) {
                logger.info("Twitter API User Info Raw Response (first 10000 chars): {}",
                        responseBody.substring(0, Math.min(10000, responseBody.length())));
            }

            // Parse JSON manually to handle potential field name issues
            com.fasterxml.jackson.databind.ObjectMapper mapper = createObjectMapper();
            TwitterUserResponse response = mapper.readValue(responseBody, TwitterUserResponse.class);

            // Log parsed response structure
            if (response != null && response.getData() != null) {
                com.socialmedia.entity.User user = response.getData();
                logger.info("Parsed User Info from API - userName={}, followers={}, following={}, favouritesCount={}, statusesCount={}, mediaCount={}",
                        user.getUserName(), user.getFollowers(), user.getFollowing(),
                        user.getFavouritesCount(), user.getStatusesCount(),
                        user.getMediaCount());
            }

            return response;
        } catch (WebClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            String responseBody = e.getResponseBodyAsString();
            logger.error("Twitter API Error Response: {}", responseBody);
            throw new TwitterApiException("Failed to get user info: " + e.getMessage(), status);
        } catch (Exception e) {
            logger.error("Exception while getting user info: {}", e.getMessage(), e);
            throw new TwitterApiException("Unexpected error while getting user info: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get user profile about by screen name.
     * Calls https://api.twitterapi.io/twitter/user_about
     * Returns about_profile with account_based_in, location_accurate, learn_more_url,
     * affiliate_username, source, username_changes.
     *
     * @see <a href="https://docs.twitterapi.io/api-reference/endpoint/get_user_about">Get User Profile About</a>
     */
    public UserAboutResponse getUserAbout(String userName) {
        try {
            logger.debug("Getting user about for: {}", userName);
            long startTime = System.currentTimeMillis();

            String encodedUserName = URLEncoder.encode(userName, StandardCharsets.UTF_8);
            URI uri = URI.create(USER_ABOUT_URL + "?userName=" + encodedUserName);

            String responseBody = webClient.get()
                    .uri(uri)
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .retry(2)
                    .doOnSubscribe(sub -> logger.debug("Starting user_about API call"))
                    .doOnSuccess(result -> logger.debug("User about API call completed in {} ms", System.currentTimeMillis() - startTime))
                    .doOnError(error -> logger.warn("User about API call failed after {} ms: {}", System.currentTimeMillis() - startTime, error.getMessage()))
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                return null;
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = createObjectMapper();
            return mapper.readValue(responseBody, UserAboutResponse.class);
        } catch (WebClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            String responseBody = e.getResponseBodyAsString();
            logger.warn("Twitter user_about API error for {}: {} - {}", userName, status, responseBody);
            throw new TwitterApiException("Failed to get user about: " + e.getMessage(), status);
        } catch (Exception e) {
            logger.warn("Exception while getting user about for {}: {}", userName, e.getMessage());
            throw new TwitterApiException("Unexpected error while getting user about: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public TwitterTweetResponse getTweets(String username, Instant startTime, Instant endTime) {
        return getTweets(username, startTime, endTime, null);
    }

    public TwitterTweetResponse getTweets(String username, Instant startTime, Instant endTime, String cursor) {
        try {
            // Build Twitter advanced search query
            // Format: from:username since:YYYY-MM-DD until:YYYY-MM-DD
            // Note: Twitter advanced search uses UTC dates in format YYYY-MM-DD
            String sinceDate = startTime != null ? DATE_FORMATTER.format(startTime) : null;
            String untilDate = endTime != null ? DATE_FORMATTER.format(endTime) : null;
            
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("from:").append(username);
            
            if (sinceDate != null) {
                queryBuilder.append(" since:").append(sinceDate);
            }
            if (untilDate != null) {
                queryBuilder.append(" until:").append(untilDate);
            }
            
            String query = queryBuilder.toString();
            
            logger.debug("Making API call - query: {}, cursor: {}", query, cursor != null ? cursor.substring(0, Math.min(20, cursor.length())) + "..." : "null");

            long apiCallStartTime = System.currentTimeMillis();
            return webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/twitter/tweet/advanced_search")
                                .queryParam("query", query)
                                .queryParam("queryType", "Latest");
                        if (cursor != null && !cursor.isEmpty()) {
                            builder.queryParam("cursor", cursor);
                        }
                        return builder.build();
                    })
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .bodyToMono(TwitterTweetResponse.class)
                    .timeout(Duration.ofSeconds(120)) // 2 minutes timeout
                    .retry(2) // Retry up to 2 times on failure
                    .doOnSubscribe(sub -> logger.debug("Starting tweets API call"))
                    .doOnSuccess(response -> logger.debug("Tweets API call succeeded in {} ms - received {} tweets", 
                            System.currentTimeMillis() - apiCallStartTime,
                            response != null && response.getData() != null ? response.getData().size() : 0))
                    .doOnError(error -> logger.warn("Tweets API call failed after {} ms: {}", 
                            System.currentTimeMillis() - apiCallStartTime, error.getMessage()))
                    .block();
        } catch (WebClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            logger.error("WebClient error - status: {}, message: {}", status, e.getMessage());
            throw new TwitterApiException("Failed to get tweets: " + e.getMessage(), status);
        } catch (Exception e) {
            logger.error("Unexpected error while getting tweets: {}", e.getMessage(), e);
            throw new TwitterApiException("Unexpected error while getting tweets: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Advanced search for tweets using keyword-based queries.
     * Supports Twitter advanced search syntax (e.g., "AI" OR "Twitter" from:elonmusk since:2021-12-31_23:59:59_UTC)
     * 
     * @param query The search query string (supports Twitter advanced search syntax)
     * @param queryType "Latest" or "Top" (default: "Latest")
     * @param cursor Cursor for pagination (null for first page)
     * @return TwitterTweetResponse containing tweets, pagination info
     */
    public TwitterTweetResponse advancedSearchTweets(String query, String queryType, String cursor) {
        try {
            // Make queryType effectively final for use in lambda
            final String finalQueryType;
            if (queryType == null || queryType.trim().isEmpty()) {
                finalQueryType = "Latest";
            } else {
                finalQueryType = queryType;
            }
            
            if (!finalQueryType.equalsIgnoreCase("Latest") && !finalQueryType.equalsIgnoreCase("Top")) {
                throw new IllegalArgumentException("queryType must be 'Latest' or 'Top'");
            }
            
            logger.debug("Advanced search - query: {}, queryType: {}, cursor: {}", 
                    query, finalQueryType, cursor != null ? cursor.substring(0, Math.min(20, cursor.length())) + "..." : "null");

            long apiCallStartTime = System.currentTimeMillis();
            TwitterTweetResponse response = webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/twitter/tweet/advanced_search")
                                .queryParam("query", query)
                                .queryParam("queryType", finalQueryType);
                        if (cursor != null && !cursor.isEmpty()) {
                            builder.queryParam("cursor", cursor);
                        }
                        return builder.build();
                    })
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .bodyToMono(TwitterTweetResponse.class)
                    .timeout(Duration.ofSeconds(120))
                    .retry(2)
                    .doOnSubscribe(sub -> logger.debug("Starting advanced search API call"))
                    .doOnSuccess(result -> logger.debug("Advanced search API call completed in {} ms - received {} tweets", 
                            System.currentTimeMillis() - apiCallStartTime,
                            result != null && result.getData() != null ? result.getData().size() : 0))
                    .doOnError(error -> logger.warn("Advanced search API call failed after {} ms: {}", 
                            System.currentTimeMillis() - apiCallStartTime, error.getMessage()))
                    .block();
            
            return response;
        } catch (WebClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            logger.error("WebClient error in advanced search - status: {}, message: {}", status, e.getMessage());
            throw new TwitterApiException("Failed to perform advanced search: " + e.getMessage(), status);
        } catch (Exception e) {
            logger.error("Unexpected error while performing advanced search: {}", e.getMessage(), e);
            throw new TwitterApiException("Unexpected error while performing advanced search: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

