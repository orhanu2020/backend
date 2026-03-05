package com.socialmedia.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordSearchEntry {
    private String keyword;
    private String searchType; // "tweet" or "user"
    private String queryType; // "Latest" or "Top" (only for tweet search)
    private String platform; // "twitter"
}
