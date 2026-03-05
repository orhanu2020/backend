package com.socialmedia.service.mapper;

import com.socialmedia.client.twitter.response.TwitterTweetResponse;
import com.socialmedia.entity.Tweet;
import com.socialmedia.entity.User;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TweetMapper {

    public Tweet toEntity(TwitterTweetResponse.TwitterTweet twitterTweet, User author) {
        if (twitterTweet == null || author == null) {
            return null;
        }

        try {
            Long tweetId = Long.parseLong(twitterTweet.getId());
            
            // Use URL from tweet if available, otherwise construct it
            String tweetUrl = twitterTweet.getUrl();
            if (tweetUrl == null || tweetUrl.isEmpty()) {
                tweetUrl = "https://twitter.com/" + author.getUserName() + "/status/" + tweetId;
            }
            
            // Map entities to JSON-compatible Map
            java.util.Map<String, Object> entitiesMap = null;
            if (twitterTweet.getEntities() != null) {
                entitiesMap = new java.util.HashMap<>();
                if (twitterTweet.getEntities().getHashtags() != null) {
                    entitiesMap.put("hashtags", twitterTweet.getEntities().getHashtags().stream()
                            .map(h -> {
                                java.util.Map<String, Object> hashtagMap = new java.util.HashMap<>();
                                hashtagMap.put("indices", h.getIndices());
                                hashtagMap.put("text", h.getText());
                                return hashtagMap;
                            })
                            .collect(java.util.stream.Collectors.toList()));
                }
                if (twitterTweet.getEntities().getUrls() != null) {
                    entitiesMap.put("urls", twitterTweet.getEntities().getUrls().stream()
                            .map(u -> {
                                java.util.Map<String, Object> urlMap = new java.util.HashMap<>();
                                urlMap.put("display_url", u.getDisplayUrl());
                                urlMap.put("expanded_url", u.getExpandedUrl());
                                urlMap.put("indices", u.getIndices());
                                urlMap.put("url", u.getUrl());
                                return urlMap;
                            })
                            .collect(java.util.stream.Collectors.toList()));
                }
                if (twitterTweet.getEntities().getUserMentions() != null) {
                    entitiesMap.put("user_mentions", twitterTweet.getEntities().getUserMentions().stream()
                            .map(um -> {
                                java.util.Map<String, Object> mentionMap = new java.util.HashMap<>();
                                mentionMap.put("id_str", um.getIdStr());
                                mentionMap.put("name", um.getName());
                                mentionMap.put("screen_name", um.getScreenName());
                                return mentionMap;
                            })
                            .collect(java.util.stream.Collectors.toList()));
                }
            }

            // Convert quoted_tweet and retweeted_tweet to Map if they're not null
            java.util.Map<String, Object> quotedTweetData = null;
            if (twitterTweet.getQuotedTweet() != null) {
                // If it's already a Map, use it; otherwise convert to Map
                if (twitterTweet.getQuotedTweet() instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) twitterTweet.getQuotedTweet();
                    quotedTweetData = map;
                } else {
                    // Use Jackson to convert to Map
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        quotedTweetData = mapper.convertValue(twitterTweet.getQuotedTweet(), 
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                    } catch (Exception e) {
                        // If conversion fails, store as null
                        quotedTweetData = null;
                    }
                }
            }

            java.util.Map<String, Object> retweetedTweetData = null;
            if (twitterTweet.getRetweetedTweet() != null) {
                if (twitterTweet.getRetweetedTweet() instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) twitterTweet.getRetweetedTweet();
                    retweetedTweetData = map;
                } else {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        retweetedTweetData = mapper.convertValue(twitterTweet.getRetweetedTweet(), 
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                    } catch (Exception e) {
                        retweetedTweetData = null;
                    }
                }
            }

            // Convert extendedEntities, card, place, and article to Map
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            
            java.util.Map<String, Object> extendedEntitiesMap = null;
            if (twitterTweet.getExtendedEntities() != null) {
                try {
                    if (twitterTweet.getExtendedEntities() instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> map = (java.util.Map<String, Object>) twitterTweet.getExtendedEntities();
                        extendedEntitiesMap = map;
                    } else {
                        extendedEntitiesMap = objectMapper.convertValue(twitterTweet.getExtendedEntities(), 
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                    }
                } catch (Exception e) {
                    extendedEntitiesMap = null;
                }
            }

            java.util.Map<String, Object> cardMap = null;
            if (twitterTweet.getCard() != null) {
                try {
                    if (twitterTweet.getCard() instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> map = (java.util.Map<String, Object>) twitterTweet.getCard();
                        cardMap = map;
                    } else {
                        cardMap = objectMapper.convertValue(twitterTweet.getCard(), 
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                    }
                } catch (Exception e) {
                    cardMap = null;
                }
            }

            java.util.Map<String, Object> placeMap = null;
            if (twitterTweet.getPlace() != null) {
                try {
                    if (twitterTweet.getPlace() instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> map = (java.util.Map<String, Object>) twitterTweet.getPlace();
                        placeMap = map;
                    } else {
                        placeMap = objectMapper.convertValue(twitterTweet.getPlace(), 
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                    }
                } catch (Exception e) {
                    placeMap = null;
                }
            }

            java.util.Map<String, Object> articleMap = null;
            if (twitterTweet.getArticle() != null) {
                try {
                    if (twitterTweet.getArticle() instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> map = (java.util.Map<String, Object>) twitterTweet.getArticle();
                        articleMap = map;
                    } else {
                        articleMap = objectMapper.convertValue(twitterTweet.getArticle(), 
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                    }
                } catch (Exception e) {
                    articleMap = null;
                }
            }

            return Tweet.builder()
                    .id(tweetId)
                    .type(twitterTweet.getType() != null ? twitterTweet.getType() : "tweet")
                    .text(twitterTweet.getText())
                    .author(author)
                    .createdAt(twitterTweet.getCreatedAtAsInstant() != null 
                            ? twitterTweet.getCreatedAtAsInstant() 
                            : Instant.now())
                    .likeCount(twitterTweet.getLikeCount() != null ? twitterTweet.getLikeCount() : 0)
                    .retweetCount(twitterTweet.getRetweetCount() != null ? twitterTweet.getRetweetCount() : 0)
                    .replyCount(twitterTweet.getReplyCount() != null ? twitterTweet.getReplyCount() : 0)
                    .quoteCount(twitterTweet.getQuoteCount() != null ? twitterTweet.getQuoteCount() : 0)
                    .viewCount(twitterTweet.getViewCount() != null ? twitterTweet.getViewCount() : 0)
                    .bookmarkCount(twitterTweet.getBookmarkCount() != null ? twitterTweet.getBookmarkCount() : 0)
                    .url(tweetUrl)
                    .twitterUrl(tweetUrl)
                    .source(twitterTweet.getSource())
                    .lang(twitterTweet.getLang())
                    .isReply(twitterTweet.getIsReply() != null ? twitterTweet.getIsReply() : false)
                    .inReplyToId(twitterTweet.getInReplyToId() != null ? Long.parseLong(twitterTweet.getInReplyToId()) : null)
                    .inReplyToUserId(twitterTweet.getInReplyToUserId() != null ? Long.parseLong(twitterTweet.getInReplyToUserId()) : null)
                    .inReplyToUsername(twitterTweet.getInReplyToUsername())
                    .conversationId(twitterTweet.getConversationId() != null ? Long.parseLong(twitterTweet.getConversationId()) : null)
                    .displayTextRange(twitterTweet.getDisplayTextRange())
                    .entities(entitiesMap)
                    .quotedTweetData(quotedTweetData)
                    .retweetedTweetData(retweetedTweetData)
                    .extendedEntities(extendedEntitiesMap)
                    .card(cardMap)
                    .place(placeMap)
                    .article(articleMap)
                    .isLimitedReply(twitterTweet.getIsLimitedReply() != null ? twitterTweet.getIsLimitedReply() : false)
                    .build();
        } catch (NumberFormatException e) {
            // If ID is not a valid Long, skip this tweet
            return null;
        }
    }
}

