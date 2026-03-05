# TwitterAPI.io Integration Documentation

Based on the official documentation at [https://docs.twitterapi.io/](https://docs.twitterapi.io/), here's what I learned about using the TwitterAPI.io service.

## Authentication

- **Method**: API Key authentication via HTTP header
- **Header Name**: `x-api-key`
- **How to Get API Key**: 
  1. Sign up at [https://twitterapi.io/](https://twitterapi.io/)
  2. Log in to your dashboard
  3. Your unique API key will be displayed on the homepage

**Example:**
```bash
curl --location 'https://api.twitterapi.io/twitter/user/followers?userName=KaitoEasyAPI' \
  --header 'x-api-key: YOUR_API_KEY'
```

## API Overview

### Base URL
- `https://api.twitterapi.io`

### Key Features
- **Stability**: Proven with over 1000K API calls
- **Performance**: Average response time of 700ms
- **High QPS**: Supports up to 200 QPS per client
- **Cost-effective pricing**:
  - $0.15 per 1,000 tweets
  - $0.18 per 1,000 user profiles
  - $0.15 per 1,000 followers
  - Minimum charge: $0.00015 per request (even if no data returned)
- **Special Offer**: Discounted rates for students and research institutions

## Available Endpoints

### User Endpoints
- `GET /twitter/user/followers` - Get user followers
- `GET /twitter/user/followings` - Get user followings
- `GET /twitter/user/last_tweets` - Get user's last tweets
- `GET /twitter/user/profile` - Get user profile
- `GET /twitter/user/search` - Search user by keyword

### Tweet Endpoints
- `GET /twitter/tweet/search` - Advanced search with filters
- `GET /twitter/tweet/replies` - Get tweet replies
- `GET /twitter/tweet/retweeters` - Get tweet retweeters

### Post & Action Endpoints (Require User Login)
- `POST /twitter/user_login_v2` - Log in as a Twitter user
- `POST /twitter/create_tweet_v2` - Create a tweet
- `POST /twitter/send_dm_to_user` - Send direct message
- `POST /twitter/like_tweet` - Like a tweet
- `POST /twitter/retweet` - Retweet

## Current Implementation Analysis

### ✅ What's Correct
1. **Authentication**: Using `x-api-key` header correctly
2. **Base URL**: Configured as `https://api.twitterapi.io`
3. **Error Handling**: Proper exception handling with `TwitterApiException`
4. **Configuration**: API key loaded from environment variable

### ⚠️ Potential Issues
The current code uses these endpoints:
- `/v2/search/users` - for searching users
- `/v2/users/{username}/tweets` - for getting tweets

However, the documentation shows endpoints like:
- `/twitter/user/search` - for searching users
- `/twitter/user/last_tweets` - for getting user tweets

**Note**: The API might support both `/v2/...` and `/twitter/...` endpoint formats, or the v2 endpoints might be a newer version. You should verify which endpoints actually work with your API key.

## Recommended Endpoints Based on Documentation

### Search Users
Based on the documentation, the endpoint should be:
```
GET /twitter/user/search?userName={username}
```

**Current Implementation:**
```java
.path("/v2/search/users")
.queryParam("q", searchString)
```

**Suggested Update (if needed):**
```java
.path("/twitter/user/search")
.queryParam("userName", searchString)
```

### Get User Tweets
Based on the documentation, the endpoint should be:
```
GET /twitter/user/last_tweets?userName={username}
```

**Current Implementation:**
```java
.path("/v2/users/{username}/tweets")
.queryParam("start_time", startTimeStr)
.queryParam("end_time", endTimeStr)
```

**Note**: The documentation shows `last_tweets` endpoint which might not support time range filtering. You may need to:
1. Use `/twitter/user/last_tweets` and filter client-side
2. Use `/twitter/tweet/search` with advanced filters
3. Verify if `/v2/users/{username}/tweets` is a valid endpoint

## Response Format

The API returns JSON responses. Example user search response structure:
```json
{
  "data": [
    {
      "username": "example_user",
      "name": "Example User",
      "profile_image_url": "https://...",
      "public_metrics": {
        "followers_count": 1000
      }
    }
  ]
}
```

## Error Handling

The API may return various HTTP status codes:
- `200` - Success
- `400` - Bad Request
- `401` - Unauthorized (invalid API key)
- `429` - Rate limit exceeded
- `500` - Server error

## Rate Limits

- Up to 200 queries per second (QPS) per client
- Monitor your usage in the dashboard

## Next Steps

1. **Verify Endpoints**: Test if the current `/v2/...` endpoints work with your API key
2. **Update if Needed**: If endpoints don't work, update to the documented `/twitter/...` format
3. **Test Response Format**: Verify the actual response structure matches your DTOs
4. **Monitor Usage**: Keep track of API calls to manage costs

## Twitter Advanced Search Operators

### Important Note
The [Twitter Advanced Search Operators](https://github.com/igorbrigadir/twitter-advanced-search) work on:
- ✅ Twitter Web Interface
- ✅ Twitter Mobile App
- ✅ TweetDeck

They do **NOT** work with:
- ❌ Twitter API v1.1
- ❌ Twitter Premium Search API
- ❌ Twitter API v2 Search

**For twitterapi.io**: These operators may or may not be supported depending on how the service implements search. You should test if the `/twitter/tweet/search` endpoint accepts these operators.

### Common Search Operators

These operators can be combined to create powerful search queries:

#### User Operators
- `from:username` - Tweets from a specific user
  - Example: `from:jack` finds tweets by @jack
- `to:username` - Tweets directed to a user (replies)
  - Example: `to:jack` finds replies to @jack
- `@username` - Tweets mentioning a user
  - Example: `@jack` finds tweets mentioning @jack

#### Content Operators
- `"exact phrase"` - Exact phrase match
  - Example: `"hello world"` finds tweets with exact phrase
- `keyword1 OR keyword2` - Either keyword
  - Example: `coffee OR tea` finds tweets with either word
- `keyword1 AND keyword2` - Both keywords
  - Example: `coffee AND morning` finds tweets with both
- `-keyword` - Exclude keyword
  - Example: `coffee -decaf` finds coffee tweets excluding decaf

#### Hashtag & Media
- `#hashtag` - Tweets with specific hashtag
  - Example: `#OpenAI` finds tweets with #OpenAI
- `filter:links` - Tweets containing links
  - Example: `news filter:links` finds news tweets with links
- `filter:media` - Tweets with media (images/videos)
  - Example: `photo filter:media` finds photo tweets with media
- `filter:images` - Tweets with images
- `filter:videos` - Tweets with videos

#### Date & Time Operators
- `since:YYYY-MM-DD` - Tweets since date
  - Example: `since:2025-01-01` finds tweets since Jan 1, 2025
- `until:YYYY-MM-DD` - Tweets until date
  - Example: `until:2025-01-31` finds tweets until Jan 31, 2025
- `since:2025-01-01 until:2025-01-31` - Date range

#### Engagement Operators
- `min_retweets:N` - Minimum retweets
  - Example: `news min_retweets:10` finds news tweets with 10+ retweets
- `min_faves:N` - Minimum likes
  - Example: `joke min_faves:100` finds jokes with 100+ likes
- `min_replies:N` - Minimum replies

#### Language & Location
- `lang:en` - Language code
  - Example: `hello lang:en` finds English tweets with "hello"
- `near:"location"` - Geographic location
  - Example: `near:"New York" within:15mi` finds tweets near NYC

#### Tweet Type Filters
- `filter:verified` - From verified accounts
- `filter:safe` - Safe content only
- `filter:nativeretweets` - Native retweets only
- `filter:replies` - Reply tweets only

### Example Combined Queries

```
from:elonmusk since:2025-01-01 filter:links
```
Finds tweets from @elonmusk since Jan 1, 2025 with links

```
#AI OR #MachineLearning min_retweets:50 lang:en
```
Finds English tweets about AI or ML with 50+ retweets

```
"climate change" -politics filter:verified since:2025-01-01
```
Finds verified tweets about climate change (excluding politics) since Jan 1

### Using with twitterapi.io

If you want to use these operators with twitterapi.io:

1. **Check Documentation**: Verify if `/twitter/tweet/search` supports these operators
2. **Pass as Query Parameter**: You might pass the full search query as a parameter
3. **Client-Side Filtering**: Alternatively, fetch tweets and filter client-side using these patterns

Example implementation idea:
```java
// If the API supports advanced search operators
String searchQuery = "from:" + username + " since:" + startDate + " until:" + endDate;
webClient.get()
    .uri(uriBuilder -> uriBuilder
        .path("/twitter/tweet/search")
        .queryParam("q", searchQuery)
        .build())
```

## References

- [Official Documentation](https://docs.twitterapi.io/)
- [Authentication Guide](https://docs.twitterapi.io/authentication)
- [API Reference](https://docs.twitterapi.io/api-reference/introduction)
- [Twitter Advanced Search Operators](https://github.com/igorbrigadir/twitter-advanced-search) - Comprehensive guide to Twitter search operators

