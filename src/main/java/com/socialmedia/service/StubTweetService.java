package com.socialmedia.service;

import com.socialmedia.entity.Tweet;
import com.socialmedia.entity.User;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * No-op TweetService when database is not configured. Persistence is skipped.
 */
@Service("tweetService")
@ConditionalOnMissingBean(name = "tweetService")
public class StubTweetService extends TweetService {

    public StubTweetService() {
        super(null);
    }

    @Override
    public List<Tweet> saveTweets(List<Tweet> tweets) {
        return Collections.emptyList();
    }

    @Override
    public List<Tweet> getTweetsByAuthorAndTimeRange(User author, Instant startTime, Instant endTime) {
        return Collections.emptyList();
    }
}
