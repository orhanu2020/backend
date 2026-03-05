package com.socialmedia.controller;

import com.socialmedia.dto.response.Tweet;
import com.socialmedia.dto.response.UserSearchResult;
import com.socialmedia.entity.User;
import com.socialmedia.service.SocialMediaService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@Validated
public class SocialMediaController {

    private final SocialMediaService socialMediaService;

    public SocialMediaController(SocialMediaService socialMediaService) {
        this.socialMediaService = socialMediaService;
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable @NotBlank String username) {
        return socialMediaService.getUserPreview(username.trim())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{username}/save")
    public ResponseEntity<User> saveUserByUsername(@PathVariable @NotBlank String username) {
        return socialMediaService.saveUserByUsername(username.trim())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{username}/tweets")
    public ResponseEntity<List<Tweet>> getTweets(
            @PathVariable @NotBlank String username,
            @RequestParam @NotBlank @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String since,
            @RequestParam @NotBlank @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String until) {
        LocalDate sinceDate = LocalDate.parse(since);
        LocalDate untilDate = LocalDate.parse(until);
        if (untilDate.isBefore(sinceDate)) {
            return ResponseEntity.badRequest().build();
        }
        Instant sinceInstant = sinceDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant untilInstant = untilDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Tweet> tweets = socialMediaService.getTweets(username.trim(), sinceInstant, untilInstant);
        return ResponseEntity.ok(tweets);
    }

    @PostMapping("/loadTwitterAccount")
    public ResponseEntity<User> loadTwitterAccount(
            @RequestParam @NotBlank(message = "Account name cannot be blank") String accountName) {
        return socialMediaService.searchUsernames(accountName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<UserSearchResult>> searchUsersByKeyword(
            @RequestParam @NotBlank(message = "Keyword cannot be blank") String keyword) {
        List<User> users = socialMediaService.searchUsersByKeyword(keyword);
        List<UserSearchResult> results = users.stream()
                .map(user -> UserSearchResult.builder()
                        .userName(user.getUserName())
                        .platform(user.getPlatformName() != null ? user.getPlatformName() : "Unknown")
                        .location(user.getLocation())
                        .createdAt(user.getCreatedAt())
                        .aboutProfile(user.getAboutProfile())
                        .build())
                .sorted((u1, u2) -> {
                    // Sort by createdAt descending (newest first)
                    // If createdAt is null, put it at the end
                    if (u1.getCreatedAt() == null && u2.getCreatedAt() == null) {
                        return 0;
                    }
                    if (u1.getCreatedAt() == null) {
                        return 1;
                    }
                    if (u2.getCreatedAt() == null) {
                        return -1;
                    }
                    return u2.getCreatedAt().compareTo(u1.getCreatedAt());
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/load-tweet")
    public ResponseEntity<Map<String, String>> loadTweet(
            @RequestParam @NotBlank(message = "Username cannot be blank") String username,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate since,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate until) {
        
        // Validation
        if (until.isBefore(since) || until.isEqual(since)) {
            throw new IllegalArgumentException("Until date must be after since date");
        }
        
        // Convert LocalDate to Instant:
        // since: on or after (inclusive) - start of day in UTC
        // until: before (NOT inclusive) - start of next day in UTC
        Instant sinceInstant = since.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant untilInstant = until.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        
        // Start async loading process
        socialMediaService.loadTweetsAsync(username, sinceInstant, untilInstant);
        
        // Return immediately
        return ResponseEntity.ok(Map.of(
            "status", "started",
            "message", "Tweet loading process started for user: " + username,
            "dateRange", since + " to " + until
        ));
    }
}

