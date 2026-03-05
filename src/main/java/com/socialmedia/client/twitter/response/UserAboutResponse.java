package com.socialmedia.client.twitter.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response from Twitter API get_user_about endpoint.
 * @see <a href="https://docs.twitterapi.io/api-reference/endpoint/get_user_about">Get User Profile About</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAboutResponse {

    @JsonProperty("data")
    private Map<String, Object> data;

    @JsonProperty("status")
    private String status;

    @JsonProperty("msg")
    private String msg;

    /**
     * Extract about_profile from data. Contains account_based_in, location_accurate,
     * learn_more_url, affiliate_username, source, username_changes (with count).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAboutProfile() {
        if (data == null) {
            return null;
        }
        Object about = data.get("about_profile");
        if (about instanceof Map) {
            return (Map<String, Object>) about;
        }
        return null;
    }
}
