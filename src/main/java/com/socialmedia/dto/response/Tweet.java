package com.socialmedia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tweet {
    private String id;
    private String text;
    private String authorUsername;
    private Instant createdAt;
    private Long likeCount;
    private Long retweetCount;
}

