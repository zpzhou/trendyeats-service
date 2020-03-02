package com.zpzhou.trendingrestaurants.search.tweets;

import com.google.gson.Gson;
import com.zpzhou.trendingrestaurants.model.TimeFrame;
import com.zpzhou.trendingrestaurants.model.Tweet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class TwitterSearchAPIClient {

    private static final Logger logger = LogManager.getLogger();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class RequestBodyContent {
        private String query;
        private String fromDate;
        private String toDate;

        RequestBodyContent(final RequestBodyContent content) {
            query = content.getQuery();
            fromDate = content.getFromDate();
            toDate = content.getToDate();
        }
    }

    @Data
    private static class PaginatedRequestBodyContent extends RequestBodyContent {
        private String next;

        PaginatedRequestBodyContent(final RequestBodyContent content, final String next) {
            super(content);
            this.next = next;
        }
    }

    private final OkHttpClient httpClient;
    private final String bearerAccessToken;
    private final String searchURL;
    private final Gson gson;

    public List<Tweet> doSearch(final String query, final TimeFrame timeFrame) {
        final RequestBodyContent initialRequestBody = getInitialRequestBodyContent(query, timeFrame);
        final Request request = buildRequest(initialRequestBody);

        final TwitterSearchQueryResponse response = sendRequest(request);
        final List<Tweet> tweets = response.getTweets();

        Optional<String> next = response.getNext();
        while (next.isPresent()) {
            final RequestBodyContent requestBody = new PaginatedRequestBodyContent(
                    initialRequestBody, next.get());
            final Request nextRequest = buildRequest(requestBody);
            final TwitterSearchQueryResponse nextResponse = sendRequest(nextRequest);
            tweets.addAll(nextResponse.getTweets());
            next = nextResponse.getNext();
        }
        return tweets;
    }

    private RequestBodyContent getInitialRequestBodyContent(final String query, final TimeFrame timeFrame) {
        final RequestBodyContent requestBodyContent = RequestBodyContent.builder()
                .query(query)
                .fromDate(getFormattedValue(timeFrame.getUTCDateTime()))
                // Clock drift occurs between our application server and Twitter's servers.
                // If we're ahead of Twitter, they will complain about our specified
                // toDate being ahead of their current time.
                .toDate(getFormattedValue(DateTime.now(DateTimeZone.UTC).minusSeconds(30)))
                .build();
        return requestBodyContent;
    }

    private Request buildRequest(final RequestBodyContent content) {
        final String jsonValue = gson.toJson(content);
        final RequestBody requestBody = RequestBody.create(jsonValue, JSON);
        final Headers headers = new Headers.Builder()
                .add("Authorization", String.format("Bearer %s", bearerAccessToken))
                .build();
        return new Request.Builder()
                .url(HttpUrl.get(searchURL))
                .headers(headers)
                .post(requestBody)
                .build();
    }

    private TwitterSearchQueryResponse sendRequest(final Request request) {
        try (final Response response = httpClient.newCall(request).execute()) {
            final Reader json = new BufferedReader(response.body().charStream());
            return gson.fromJson(json, TwitterSearchQueryResponse.class);
        }
        catch (final IOException ex) {
            logger.warn("Failed to send search request {}", ex);
            throw new RuntimeException(ex.getCause());
        }
    }

    private String getFormattedValue(final DateTime dateTime) {
        return dateTime.toString("yyyyMMddHHmm");
    }
}
