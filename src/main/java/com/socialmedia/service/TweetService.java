package com.socialmedia.service;

import com.socialmedia.entity.Tweet;
import com.socialmedia.entity.User;
import com.socialmedia.repository.TweetRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnBean(DataSource.class)
public class TweetService {

    private final TweetRepository tweetRepository;

    public TweetService(TweetRepository tweetRepository) {
        this.tweetRepository = tweetRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Tweet> saveTweets(List<Tweet> tweets) {
        // Filter out null tweets and save only new ones
        List<Tweet> validTweets = tweets.stream()
                .filter(tweet -> tweet != null && tweet.getId() != null)
                .collect(Collectors.toList());
        
        if (validTweets.isEmpty()) {
            return validTweets;
        }
        
        // Collect all tweet IDs in this batch
        java.util.Set<Long> batchTweetIds = validTweets.stream()
                .map(Tweet::getId)
                .collect(java.util.stream.Collectors.toSet());
        
        // Separate tweets into existing (to update) and new (to insert)
        List<Tweet> tweetsToUpdate = new java.util.ArrayList<>();
        List<Tweet> newTweets = new java.util.ArrayList<>();
        
        for (Tweet tweet : validTweets) {
            if (tweetRepository.existsById(tweet.getId())) {
                // Update existing tweet
                Tweet existing = tweetRepository.findById(tweet.getId()).orElse(tweet);
                existing.setText(tweet.getText());
                existing.setLikeCount(tweet.getLikeCount());
                existing.setRetweetCount(tweet.getRetweetCount());
                existing.setReplyCount(tweet.getReplyCount());
                existing.setQuoteCount(tweet.getQuoteCount());
                existing.setViewCount(tweet.getViewCount());
                existing.setBookmarkCount(tweet.getBookmarkCount());
                existing.setUpdatedAt(Instant.now());
                tweetsToUpdate.add(existing);
            } else {
                // New tweet - check if it's a reply and if parent exists
                if (tweet.getInReplyToId() != null) {
                    boolean parentExists = tweetRepository.existsById(tweet.getInReplyToId()) 
                            || batchTweetIds.contains(tweet.getInReplyToId());
                    
                    if (!parentExists) {
                        // Parent tweet doesn't exist - set in_reply_to_id to null to avoid FK constraint violation
                        tweet.setInReplyToId(null);
                    }
                }
                newTweets.add(tweet);
            }
        }
        
        // Save tweets in correct order to handle foreign key constraints:
        // 1. First, save non-reply tweets (or tweets whose parents already exist in DB)
        // 2. Then, save reply tweets whose parents are in the current batch
        List<Tweet> savedTweets = new java.util.ArrayList<>();
        
        // Save updated tweets first
        for (Tweet tweet : tweetsToUpdate) {
            savedTweets.add(tweetRepository.save(tweet));
        }
        
        // Separate new tweets into non-replies and replies
        List<Tweet> nonReplyTweets = new java.util.ArrayList<>();
        List<Tweet> replyTweets = new java.util.ArrayList<>();
        
        for (Tweet tweet : newTweets) {
            if (tweet.getInReplyToId() == null) {
                nonReplyTweets.add(tweet);
            } else {
                replyTweets.add(tweet);
            }
        }
        
        // Save non-reply tweets first
        for (Tweet tweet : nonReplyTweets) {
            savedTweets.add(tweetRepository.save(tweet));
        }
        
        // Now save reply tweets (their parents should now exist either in DB or in savedTweets)
        for (Tweet tweet : replyTweets) {
            // Double-check parent exists before saving
            if (tweet.getInReplyToId() != null) {
                boolean parentExists = tweetRepository.existsById(tweet.getInReplyToId())
                        || savedTweets.stream().anyMatch(t -> t.getId().equals(tweet.getInReplyToId()));
                
                if (!parentExists) {
                    // Still doesn't exist, remove the reference
                    tweet.setInReplyToId(null);
                }
            }
            savedTweets.add(tweetRepository.save(tweet));
        }
        
        return savedTweets;
    }

    public List<Tweet> getTweetsByAuthorAndTimeRange(User author, Instant startTime, Instant endTime) {
        return tweetRepository.findTweetsByAuthorAndTimeRange(author.getId(), startTime, endTime);
    }
}

