package com.socialmedia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private List<Tweet> tweets;
    private Boolean hasNextPage;
    private String nextCursor;
    private Integer count;
}
