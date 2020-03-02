package com.zpzhou.trendingrestaurants.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class Tweet {
    private final String text;
    private final List<String> hashTags;
    private final int retweetCount;
    private final int favoriteCount;
    private final String createdAt;
    private final String twitterHandle;
    private final Optional<LatLng> latLng;
}
