package com.socialmedia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResult {
    private String userName;
    private String platform;
    private String location;
    private Instant createdAt;
    /** About profile from Twitter get_user_about (account_based_in, location_accurate, learn_more_url, affiliate_username, source, username_changes). */
    private Map<String, Object> aboutProfile;
}

