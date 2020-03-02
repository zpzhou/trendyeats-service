package com.zpzhou.trendingrestaurants.model;

import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

@RequiredArgsConstructor
public enum TimeFrame {
    ONE_DAY("one-day", 1),
    THREE_DAY("three-day", 3),
    WEEK("week", 7);

    private final String stringValue;
    private final int durationDays;

    /**
     * Returns the UTC DateTime resulting from the current date
     * subtracted by durationDays.
     */
    public DateTime getUTCDateTime() {
        return DateTime.now(DateTimeZone.UTC)
                .minusDays(durationDays);
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
