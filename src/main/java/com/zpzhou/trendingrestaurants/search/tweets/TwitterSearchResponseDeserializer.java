package com.zpzhou.trendingrestaurants.search.tweets;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.zpzhou.trendingrestaurants.model.LatLng;
import com.zpzhou.trendingrestaurants.model.Tweet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TwitterSearchResponseDeserializer implements JsonDeserializer<TwitterSearchQueryResponse> {

    private static final Logger logger = LogManager.getLogger();

    @Override
    public TwitterSearchQueryResponse deserialize(final JsonElement jsonElement,
                                                  final Type type,
                                                  final JsonDeserializationContext jsonDeserializationContext
    ) throws JsonParseException {
        // extract tweets
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        final JsonArray resultsJson = jsonObject.getAsJsonArray("results");
        final List<Tweet> tweets = new ArrayList<>();

        try {
            for (final JsonElement el : resultsJson) {
                // extract flat fields
                final JsonObject tweetJson = el.getAsJsonObject();
                final Tweet.TweetBuilder tweetBuilder = Tweet.builder()
                        .text(tweetJson.get("text").getAsString())
                        .retweetCount(tweetJson.get("retweet_count").getAsInt())
                        .favoriteCount(tweetJson.get("favorite_count").getAsInt())
                        .twitterHandle(tweetJson.getAsJsonObject("user").get("screen_name").getAsString())
                        .twitterHandle(tweetJson.get("created_at").getAsString());

                // extract hash tags
                final List<String> hashTags = new ArrayList<>();
                final JsonArray hashTagsJson = tweetJson.getAsJsonObject("entities").getAsJsonArray("hashtags");
                for (JsonElement hashTagJsonEl : hashTagsJson) {
                        final JsonObject hashTagJson = hashTagJsonEl.getAsJsonObject();
                        hashTags.add(hashTagJson.get("text").getAsString());
                }
                tweetBuilder.hashTags(hashTags);

                // check if geo information available for given tweet
                final Optional<LatLng> latLng;
                if (!tweetJson.get("geo").isJsonNull()) {
                    final JsonArray coords = tweetJson.getAsJsonObject("geo").getAsJsonArray("coordinates");
                    latLng = Optional.of(new LatLng(
                            coords.get(0).getAsBigDecimal(), coords.get(1).getAsBigDecimal()));
                } else {
                    latLng = Optional.empty();
                }
                tweets.add(tweetBuilder
                        .latLng(latLng)
                        .build());
            }
            // check if response has a 'next' token
            final Optional<String> next = jsonObject.has("next") ?
                    Optional.of(jsonObject.get("next").getAsString()) : Optional.empty();

            return new TwitterSearchQueryResponse(tweets, next);
        }
        catch (final NullPointerException ex) {
            logger.warn("Caught exception '{}' parsing twitter API response:\n\t {}",
                    ex.getMessage(), jsonObject.toString());
            return new TwitterSearchQueryResponse(Collections.emptyList(), Optional.empty());
        }
    }
}
