package com.socialmedia.client.twitter.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.socialmedia.entity.User;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwitterTweetResponse {
    @JsonProperty("tweets")
    private List<TwitterTweet> tweets;

    @JsonProperty("has_next_page")
    private Boolean hasNextPage;

    @JsonProperty("next_cursor")
    private String nextCursor;

    /**
     * Get tweets list - returns tweets array
     * This method provides backward compatibility with code expecting getData()
     */
    public List<TwitterTweet> getData() {
        return tweets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TwitterTweet {
        @JsonProperty("type")
        private String type;

        @JsonProperty("id")
        private String id;

        @JsonProperty("url")
        private String url;

        @JsonProperty("text")
        private String text;

        @JsonProperty("source")
        private String source;

        @JsonProperty("retweetCount")
        private Integer retweetCount;

        @JsonProperty("replyCount")
        private Integer replyCount;

        @JsonProperty("likeCount")
        private Integer likeCount;

        @JsonProperty("quoteCount")
        private Integer quoteCount;

        @JsonProperty("viewCount")
        private Integer viewCount;

        @JsonProperty("createdAt")
        private String createdAt;

        @JsonProperty("lang")
        private String lang;

        @JsonProperty("bookmarkCount")
        private Integer bookmarkCount;

        @JsonProperty("isReply")
        private Boolean isReply;

        @JsonProperty("inReplyToId")
        private String inReplyToId;

        @JsonProperty("conversationId")
        private String conversationId;

        @JsonProperty("displayTextRange")
        private List<Integer> displayTextRange;

        @JsonProperty("inReplyToUserId")
        private String inReplyToUserId;

        @JsonProperty("inReplyToUsername")
        private String inReplyToUsername;

        @JsonProperty("author")
        private User author;

        @JsonProperty("entities")
        private TweetEntities entities;

        @JsonProperty("quoted_tweet")
        private Object quotedTweet; // Can be a full tweet object or null

        @JsonProperty("retweeted_tweet")
        private Object retweetedTweet; // Can be a full tweet object or null

        @JsonProperty("isLimitedReply")
        private Boolean isLimitedReply;

        @JsonProperty("extendedEntities")
        private Object extendedEntities;

        @JsonProperty("card")
        private Object card;

        @JsonProperty("place")
        private Object place;

        @JsonProperty("article")
        private Object article;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TweetEntities {
            @JsonProperty("hashtags")
            private List<Hashtag> hashtags;

            @JsonProperty("urls")
            private List<Url> urls;

            @JsonProperty("user_mentions")
            private List<UserMention> userMentions;

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Hashtag {
                @JsonProperty("indices")
                private List<Integer> indices;

                @JsonProperty("text")
                private String text;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Url {
                @JsonProperty("display_url")
                private String displayUrl;

                @JsonProperty("expanded_url")
                private String expandedUrl;

                @JsonProperty("indices")
                private List<Integer> indices;

                @JsonProperty("url")
                private String url;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class UserMention {
                @JsonProperty("id_str")
                private String idStr;

                @JsonProperty("name")
                private String name;

                @JsonProperty("screen_name")
                private String screenName;
            }
        }

        /**
         * Get createdAt as Instant
         * The API returns createdAt as a string in Twitter date format, so we parse it
         * Format: "Mon Nov 20 11:53:28 +0000 2017"
         */
        public Instant getCreatedAtAsInstant() {
            if (createdAt == null || createdAt.isEmpty()) {
                return null;
            }
            try {
                // Try Twitter date format: "EEE MMM dd HH:mm:ss Z yyyy"
                // Example: "Mon Nov 20 11:53:28 +0000 2017"
                java.time.format.DateTimeFormatter twitterFormatter = new java.time.format.DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("EEE MMM dd HH:mm:ss Z yyyy")
                        .toFormatter(java.util.Locale.ENGLISH);
                return java.time.ZonedDateTime.parse(createdAt, twitterFormatter)
                        .toInstant();
            } catch (Exception e) {
                try {
                    // Try ISO-8601 format
                    return Instant.parse(createdAt);
                } catch (Exception e2) {
                    try {
                        // Try format without timezone
                        java.time.format.DateTimeFormatter formatter = 
                                java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy", java.util.Locale.ENGLISH);
                        return java.time.LocalDateTime.parse(createdAt, formatter)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toInstant();
                    } catch (Exception e3) {
                        return null;
                    }
                }
            }
        }

        /**
         * Get likeCount as Long for backward compatibility
         */
        public Long getLikeCountAsLong() {
            return likeCount != null ? likeCount.longValue() : 0L;
        }

        /**
         * Get retweetCount as Long for backward compatibility
         */
        public Long getRetweetCountAsLong() {
            return retweetCount != null ? retweetCount.longValue() : 0L;
        }
    }
}

