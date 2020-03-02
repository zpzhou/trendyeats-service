package com.zpzhou.trendingrestaurants.model;

import lombok.Data;
import org.joda.time.DateTime;

import java.util.List;

@Data
public class TrendsList {
    private final int numberTrends;
    private final DateTime dateTime;
    private final TimeFrame timeFrame;
    private final List<Trend> trends;
}
