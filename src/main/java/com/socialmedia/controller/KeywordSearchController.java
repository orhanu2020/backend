package com.socialmedia.controller;

import com.socialmedia.dto.response.Tweet;
import com.socialmedia.dto.response.UserSearchResult;
import com.socialmedia.dto.response.SearchResult;
import com.socialmedia.entity.User;
import com.socialmedia.model.KeywordSearchEntry;
import com.socialmedia.service.KeywordSearchService;
import com.socialmedia.service.twitter.TwitterService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/keyword-search")
@Validated
public class KeywordSearchController {

    private final KeywordSearchService keywordSearchService;
    private final TwitterService twitterService;

    public KeywordSearchController(KeywordSearchService keywordSearchService,
                              TwitterService twitterService) {
        this.keywordSearchService = keywordSearchService;
        this.twitterService = twitterService;
    }

    @GetMapping("/entries")
    public ResponseEntity<List<KeywordSearchEntry>> getAllEntries() {
        return ResponseEntity.ok(keywordSearchService.getAllEntries());
    }

    @PostMapping("/entries")
    public ResponseEntity<Map<String, String>> addEntry(
            @RequestParam @NotBlank(message = "Keyword cannot be blank") String keyword,
            @RequestParam @NotBlank(message = "Search type cannot be blank") String searchType,
            @RequestParam(required = false) String queryType,
            @RequestParam(required = false, defaultValue = "twitter") String platform) {
        
        // Validate search type
        if (!searchType.equalsIgnoreCase("tweet") && !searchType.equalsIgnoreCase("user")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Search type must be 'tweet' or 'user'"
            ));
        }
        
        // Validate query type for tweet search
        if (searchType.equalsIgnoreCase("tweet")) {
            if (queryType == null || queryType.trim().isEmpty()) {
                queryType = "Latest";
            }
            if (!queryType.equalsIgnoreCase("Latest") && !queryType.equalsIgnoreCase("Top")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Query type must be 'Latest' or 'Top' for tweet search"
                ));
            }
        }
        
        KeywordSearchEntry entry = KeywordSearchEntry.builder()
                .keyword(keyword)
                .searchType(searchType.toLowerCase())
                .queryType(queryType)
                .platform(platform)
                .build();
        
        try {
            keywordSearchService.addEntry(entry);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Entry added successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/entries/multiline")
    public ResponseEntity<Map<String, Object>> addEntriesFromMultiline(
            @RequestParam @NotBlank(message = "Multiline text cannot be blank") String multilineText) {
        
        List<KeywordSearchEntry> entries = keywordSearchService.parseMultilineText(multilineText);
        
        if (entries.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "No valid entries found in multiline text"
            ));
        }
        
        keywordSearchService.addEntries(entries);
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Entries added successfully",
                "count", entries.size()
        ));
    }

    @DeleteMapping("/entries/{index}")
    public ResponseEntity<Map<String, String>> removeEntry(@PathVariable int index) {
        try {
            keywordSearchService.removeEntry(index);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Entry removed successfully"
            ));
        } catch (IndexOutOfBoundsException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid index: " + index
            ));
        }
    }

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam @NotBlank(message = "Keyword cannot be blank") String keyword,
            @RequestParam @NotBlank(message = "Search type cannot be blank") String searchType,
            @RequestParam(required = false) String queryType,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        
        if (!searchType.equalsIgnoreCase("tweet") && !searchType.equalsIgnoreCase("user")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Search type must be 'tweet' or 'user'"
            ));
        }
        
        try {
            if (searchType.equalsIgnoreCase("tweet")) {
                if (queryType == null || queryType.trim().isEmpty()) {
                    queryType = "Latest";
                }
                SearchResult searchResult = twitterService.searchTweetsByKeyword(keyword, queryType, cursor, since, until);
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "searchType", "tweet",
                        "keyword", keyword,
                        "queryType", queryType,
                        "count", searchResult.getCount(),
                        "results", searchResult.getTweets(),
                        "hasNextPage", searchResult.getHasNextPage() != null ? searchResult.getHasNextPage() : false,
                        "nextCursor", searchResult.getNextCursor() != null ? searchResult.getNextCursor() : ""
                ));
            } else {
                List<User> users = twitterService.searchUsersByKeyword(keyword);
                List<UserSearchResult> results = users.stream()
                        .map(user -> UserSearchResult.builder()
                                .userName(user.getUserName())
                                .platform(user.getPlatformName() != null ? user.getPlatformName() : "Unknown")
                                .location(user.getLocation())
                                .createdAt(user.getCreatedAt())
                                .aboutProfile(user.getAboutProfile())
                                .build())
                        .collect(Collectors.toList());
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "searchType", "user",
                        "keyword", keyword,
                        "count", results.size(),
                        "results", results,
                        "hasNextPage", false,
                        "nextCursor", ""
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Search failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/search/async")
    public ResponseEntity<Map<String, String>> searchAsync(
            @RequestParam @NotBlank(message = "Keyword cannot be blank") String keyword,
            @RequestParam @NotBlank(message = "Search type cannot be blank") String searchType,
            @RequestParam(required = false) String queryType) {
        
        if (!searchType.equalsIgnoreCase("tweet") && !searchType.equalsIgnoreCase("user")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Search type must be 'tweet' or 'user'"
            ));
        }
        
        try {
            if (searchType.equalsIgnoreCase("tweet")) {
                if (queryType == null || queryType.trim().isEmpty()) {
                    queryType = "Latest";
                }
                twitterService.loadTweetsByKeywordAsync(keyword, queryType);
                return ResponseEntity.ok(Map.of(
                        "status", "started",
                        "message", "Tweet search process started for keyword: " + keyword,
                        "keyword", keyword,
                        "queryType", queryType
                ));
            } else {
                // For user search, we can use the existing searchUsersByKeyword which is synchronous
                // If needed, we could make it async in the future
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Async user search is not yet supported. Use /search endpoint instead."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Search failed: " + e.getMessage()
            ));
        }
    }
}
