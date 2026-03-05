package com.socialmedia.service;

import com.socialmedia.dto.response.Tweet;
import com.socialmedia.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SocialMediaService {
    /**
     * Get user by username from the database.
     *
     * @param username the username (handle) of the user
     * @return Optional containing the user if found, or empty
     */
    Optional<User> getUserByUsername(String username);

    /**
     * Get user by username: from database if present, otherwise fetch from API without saving.
     * Use this when displaying user details so the client can choose to save via Save button.
     *
     * @param username the username (handle) of the user
     * @return Optional containing the user if found in DB or from API, or empty
     */
    Optional<User> getUserPreview(String username);

    /**
     * Fetch user from API and save to database. Use when client explicitly requests save.
     *
     * @param username the username (handle) of the user
     * @return Optional containing the saved user, or empty if not found
     */
    Optional<User> saveUserByUsername(String username);

    /**
     * Search for a user matching the given search string
     *
     * @param searchString the search query string
     * @return Optional containing the first user found, or empty if no user found
     */
    Optional<User> searchUsernames(String searchString);

    /**
     * Search for users by keyword (searches across all user fields)
     * Returns a list of User entities without saving to database
     *
     * @param keyword the keyword to search for
     * @return list of User entities matching the keyword
     */
    List<User> searchUsersByKeyword(String keyword);

    /**
     * Get all tweets from a specific user within a time range
     *
     * @param username  the username of the user
     * @param startTime the start time (UTC)
     * @param endTime   the end time (UTC)
     * @return list of tweets
     */
    List<Tweet> getTweets(String username, Instant startTime, Instant endTime);

    /**
     * Asynchronously load all tweets for a user within a date range.
     * This method handles pagination and loads all available tweets to the database.
     *
     * @param username  the username of the user
     * @param startTime the start time (UTC)
     * @param endTime   the end time (UTC)
     */
    void loadTweetsAsync(String username, Instant startTime, Instant endTime);
}

