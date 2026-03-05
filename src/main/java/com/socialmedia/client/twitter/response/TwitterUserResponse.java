package com.socialmedia.client.twitter.response;

import com.socialmedia.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwitterUserResponse {
    @JsonProperty("users")
    private List<User> users;

    @JsonProperty("data")
    private User data;

    @JsonProperty("has_next_page")
    private Boolean hasNextPage;

    @JsonProperty("next_cursor")
    private String nextCursor;

    @JsonProperty("status")
    private String status;

    @JsonProperty("msg")
    private String msg;

    /**
     * Get users from either the users array or the data field
     * This handles both response formats:
     * - Keyword search: returns users array
     * - Username lookup: returns single user in data field
     */
    public List<User> getUsersList() {
        if (users != null && !users.isEmpty()) {
            return users;
        }
        if (data != null) {
            return List.of(data);
        }
        return List.of();
    }
}

