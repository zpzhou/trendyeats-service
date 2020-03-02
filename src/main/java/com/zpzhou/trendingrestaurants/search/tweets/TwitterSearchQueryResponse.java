package com.zpzhou.trendingrestaurants.search.tweets;

import com.zpzhou.trendingrestaurants.model.Tweet;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class TwitterSearchQueryResponse {
    private final List<Tweet> tweets;
    private final Optional<String> next;
}
