package com.socialmedia.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.socialmedia.client.twitter.response.TwitterDateDeserializer;
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
@Table(name = "users", schema = "socialmedia", indexes = {
    @Index(name = "idx_users_user_name", columnList = "user_name"),
    @Index(name = "idx_users_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "platform_id", length = 100)
    private String platformId;

    // Map Twitter API's "id" field to platformId during deserialization
    @JsonSetter("id")
    public void setPlatformIdFromId(Object id) {
        if (id == null) {
            this.platformId = null;
        } else if (id instanceof String) {
            this.platformId = (String) id;
        } else {
            // Handle numeric IDs (Long, Integer, etc.)
            this.platformId = String.valueOf(id);
        }
    }

    // Map platformId to "id" in JSON responses
    @JsonGetter("id")
    public String getPlatformIdAsId() {
        return platformId;
    }

    @Column(name = "platform_name", length = 50)
    private String platformName;

    @Column(name = "user_name", nullable = false, length = 255)
    private String userName;

    // Map Twitter API's "screen_name" field to userName during deserialization
    // Based on actual API response: {"screen_name": "gulen_mesih", ...}
    @JsonSetter("screen_name")
    public void setUserNameFromScreenName(String screenName) {
        if (screenName != null && !screenName.trim().isEmpty()) {
            this.userName = screenName.trim();
        }
    }

    // Map Twitter API's "userName" field (camelCase) to userName
    @JsonSetter("userName")
    public void setUserNameFromUserName(String userName) {
        if (userName != null && !userName.trim().isEmpty()) {
            this.userName = userName.trim();
        }
    }

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "twitter_url", columnDefinition = "TEXT")
    private String twitterUrl;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_blue_verified")
    @Builder.Default
    private Boolean isBlueVerified = false;

    @Column(name = "verified_type", length = 50)
    private String verifiedType;

    @Column(name = "profile_picture", columnDefinition = "TEXT")
    private String profilePicture;

    @Column(name = "cover_picture", columnDefinition = "TEXT")
    private String coverPicture;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "followers")
    @JsonProperty("followers")
    @com.fasterxml.jackson.annotation.JsonAlias({"followers_count", "followersCount"})
    @Builder.Default
    private Integer followers = 0;

    @JsonSetter("followers_count")
    public void setFollowersFromCount(Integer count) {
        this.followers = count != null ? count : 0;
    }

    @Column(name = "following")
    @JsonProperty("following")
    @com.fasterxml.jackson.annotation.JsonAlias({"following_count", "followingCount", "friends_count", "friendsCount"})
    @Builder.Default
    private Integer following = 0;

    @JsonSetter("following_count")
    public void setFollowingFromCount(Integer count) {
        this.following = count != null ? count : 0;
    }

    @JsonSetter("friends_count")
    public void setFollowingFromFriendsCount(Integer count) {
        this.following = count != null ? count : 0;
    }

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "can_dm")
    @Builder.Default
    private Boolean canDm = false;

    @Column(name = "can_media_tag")
    @Builder.Default
    private Boolean canMediaTag = true;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    @com.fasterxml.jackson.annotation.JsonAlias({"createdAt"})
    @JsonDeserialize(using = TwitterDateDeserializer.class)
    private Instant createdAt;

    @Column(name = "fast_followers_count")
    @Builder.Default
    private Integer fastFollowersCount = 0;

    @Column(name = "favourites_count")
    @JsonProperty("favouritesCount")
    @com.fasterxml.jackson.annotation.JsonAlias({"favourites_count", "favorite_count", "favoriteCount", "likes_count", "likesCount"})
    @Builder.Default
    private Integer favouritesCount = 0;

    @JsonSetter("favourites_count")
    public void setFavouritesCountFromSnakeCase(Integer count) {
        this.favouritesCount = count != null ? count : 0;
    }

    @Column(name = "has_custom_timelines")
    @Builder.Default
    private Boolean hasCustomTimelines = false;

    @Column(name = "is_translator")
    @Builder.Default
    private Boolean isTranslator = false;

    @Column(name = "media_count")
    @JsonProperty("mediaCount")
    @com.fasterxml.jackson.annotation.JsonAlias({"media_tweets_count", "media_count", "mediaCount", "mediaTweetsCount"})
    @Builder.Default
    private Integer mediaCount = 0;

    @JsonSetter("media_tweets_count")
    public void setMediaCountFromTweetsCount(Integer count) {
        this.mediaCount = count != null ? count : 0;
    }

    @Column(name = "statuses_count")
    @JsonProperty("statusesCount")
    @com.fasterxml.jackson.annotation.JsonAlias({"statuses_count", "statusesCount", "tweet_count", "tweetCount"})
    @Builder.Default
    private Integer statusesCount = 0;

    @JsonSetter("statuses_count")
    public void setStatusesCountFromSnakeCase(Integer count) {
        this.statusesCount = count != null ? count : 0;
    }

    @Column(name = "possibly_sensitive")
    @Builder.Default
    private Boolean possiblySensitive = false;

    @Column(name = "is_automated")
    @Builder.Default
    private Boolean isAutomated = false;

    @Column(name = "automated_by", length = 255)
    private String automatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_bio", columnDefinition = "jsonb")
    private Map<String, Object> profileBio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entities", columnDefinition = "jsonb")
    private Map<String, Object> entities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "withheld_in_countries", columnDefinition = "jsonb")
    private List<String> withheldInCountries;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affiliates_highlighted_label", columnDefinition = "jsonb")
    private Map<String, Object> affiliatesHighlightedLabel;

    /**
     * User profile "about" section from Twitter API get_user_about.
     * Contains account_based_in, location_accurate, learn_more_url,
     * affiliate_username, source, username_changes (with count).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "about_profile", columnDefinition = "jsonb")
    private Map<String, Object> aboutProfile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pinned_tweet_ids", columnDefinition = "jsonb")
    private List<Long> pinnedTweetIds;

    @CreationTimestamp
    @Column(name = "first_seen_at", updatable = false)
    private Instant firstSeenAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "author", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    private List<Tweet> tweets;
}

