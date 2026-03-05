package com.socialmedia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsernameSearchResult {
    private String username;
    private String displayName;
    private String profileUrl;
    private Long followerCount;
    private List<UsernameHistoryItem> activationHistory;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsernameHistoryItem {
        private String username;
        private Instant validFrom;
        private Instant validTo;
    }
}
