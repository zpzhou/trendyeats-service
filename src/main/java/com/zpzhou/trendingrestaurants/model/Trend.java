package com.zpzhou.trendingrestaurants.model;

import com.google.maps.model.PlaceDetails;
import lombok.Data;

@Data
public class Trend implements Comparable<Trend> {
    private final PlaceDetails details;
    private final int totalTweets;
    private final int totalRetweets;
    private final int totalFavorites;

    /**
     * Will throw NPE if @param obj is null
     */
    @Override
    public int compareTo(final Trend obj) {
        final int[] A = {getTotalTweets(), getTotalFavorites(), getTotalRetweets()};
        final int[] B = {obj.getTotalTweets(), obj.getTotalFavorites(), obj.getTotalRetweets()};
        for (int i = 0; i < 3; i++) {
            if (A[i] > B[i]) {
                return -1;
            }
            else if (A[i] < B[i]) {
                return 1;
            }
        }
        return 0;
    }
}
