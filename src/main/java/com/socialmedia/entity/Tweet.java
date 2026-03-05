package com.socialmedia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "tweets", schema = "socialmedia", indexes = {
    @Index(name = "idx_tweets_author_id", columnList = "author_id"),
    @Index(name = "idx_tweets_created_at", columnList = "created_at"),
    @Index(name = "idx_tweets_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_tweets_in_reply_to_id", columnList = "in_reply_to_id"),
    @Index(name = "idx_tweets_lang", columnList = "lang")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tweet {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "type", length = 50)
    @Builder.Default
    private String type = "tweet";

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "twitter_url", columnDefinition = "TEXT")
    private String twitterUrl;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "retweet_count")
    @Builder.Default
    private Integer retweetCount = 0;

    @Column(name = "reply_count")
    @Builder.Default
    private Integer replyCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "quote_count")
    @Builder.Default
    private Integer quoteCount = 0;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "bookmark_count")
    @Builder.Default
    private Integer bookmarkCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "lang", length = 10)
    private String lang;

    @Column(name = "is_reply")
    @Builder.Default
    private Boolean isReply = false;

    @Column(name = "in_reply_to_id")
    private Long inReplyToId;

    @Column(name = "in_reply_to_user_id")
    private Long inReplyToUserId;

    @Column(name = "in_reply_to_username", length = 255)
    private String inReplyToUsername;

    @Column(name = "conversation_id")
    private Long conversationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "display_text_range", columnDefinition = "jsonb")
    private List<Integer> displayTextRange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_author"))
    private User author;

    @Column(name = "is_limited_reply")
    @Builder.Default
    private Boolean isLimitedReply = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_entities", columnDefinition = "jsonb")
    private Map<String, Object> extendedEntities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "card", columnDefinition = "jsonb")
    private Map<String, Object> card;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "place", columnDefinition = "jsonb")
    private Map<String, Object> place;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entities", columnDefinition = "jsonb")
    private Map<String, Object> entities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "article", columnDefinition = "jsonb")
    private Map<String, Object> article;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quoted_tweet_data", columnDefinition = "jsonb")
    private Map<String, Object> quotedTweetData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "retweeted_tweet_data", columnDefinition = "jsonb")
    private Map<String, Object> retweetedTweetData;

    @CreationTimestamp
    @Column(name = "fetched_at", updatable = false)
    private Instant fetchedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_reply_to_id", insertable = false, updatable = false)
    private Tweet inReplyToTweet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_reply_to_user_id", insertable = false, updatable = false)
    private User inReplyToUser;

}

