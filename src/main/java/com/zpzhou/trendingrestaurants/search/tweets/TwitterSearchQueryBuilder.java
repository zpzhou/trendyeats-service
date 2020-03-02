package com.zpzhou.trendingrestaurants.search.tweets;

import java.util.Collection;

public class TwitterSearchQueryBuilder {

    private final StringBuilder query = new StringBuilder();

    public String build() {
        return query.toString();
    }

    public TwitterSearchQueryBuilder withAllKeywords(final Collection<String> keywords) {
        query.append(String.format("(%s) ", String.join(" AND ", keywords)));
        return this;
    }

    public TwitterSearchQueryBuilder withAnyKeywords(final Collection<String> keywords) {
        query.append(String.format("(%s)", String.join(" OR ", keywords)));
        return this;
    }

    public TwitterSearchQueryBuilder withHashTag(final String hashTag) {
        query.append(String.format("#%s ", hashTag));
        return this;
    }

    public TwitterSearchQueryBuilder withGeo() {
        query.append("has:geo ");
        return this;
    }

    public TwitterSearchQueryBuilder withPlace(final String place) {
        query.append(String.format("place:\"%s\" ", place));
        return this;
    }
}
