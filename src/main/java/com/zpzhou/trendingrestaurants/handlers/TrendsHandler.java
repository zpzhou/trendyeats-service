package com.zpzhou.trendingrestaurants.handlers;

import com.google.maps.model.PlaceDetails;
import com.zpzhou.trendingrestaurants.model.TimeFrame;
import com.zpzhou.trendingrestaurants.model.Trend;
import com.zpzhou.trendingrestaurants.model.TrendsList;
import com.zpzhou.trendingrestaurants.model.Tweet;
import com.zpzhou.trendingrestaurants.search.places.BusinessNameFinderTask;
import com.zpzhou.trendingrestaurants.search.places.GoogleMapsAPIWrapper;
import com.zpzhou.trendingrestaurants.search.tweets.TwitterSearchQueryBuilder;
import com.zpzhou.trendingrestaurants.search.tweets.TwitterSearchAPIClient;
import lombok.RequiredArgsConstructor;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TrendsHandler {

    private static final Logger logger = LogManager.getLogger();

    private final TwitterSearchAPIClient twitterSearchAPIClient;
    private final GoogleMapsAPIWrapper mapsAPIWrapper;
    private final ExecutorService executorService;
    private final TokenNameFinderModel nameFinderModel;
    private final Collection<String> foodKeywords;

    public TrendsList handleGet(final String place, final TimeFrame timeFrame) throws InterruptedException {
        // Query for tweets
        final String query = new TwitterSearchQueryBuilder()
                .withAnyKeywords(foodKeywords)
                .withPlace(place)
                .build();
        final List<Tweet> tweets = twitterSearchAPIClient.doSearch(query, timeFrame);

        // Search for places mentioned in returned tweets
        final List<Callable<Optional<PlaceDetails>>> findPlaceTasks = tweets.stream()
                .map(this::createFindPlaceTask)
                .collect(Collectors.toList());
        @SuppressWarnings("unchecked")
        final List<Optional<PlaceDetails>> places = executorService.invokeAll(findPlaceTasks)
                .stream()
                .map(future -> {
                    try {
                        return future.get();
                    }
                    catch (final Exception ex) {
                        logger.warn("Failed to fetch place details {}", ex.getMessage());
                        return Optional.empty();
                    }
                })
                .map(optional -> (Optional<PlaceDetails>) optional)
                .collect(Collectors.toList());

        // Aggregate tweets and places to find trends
        final List<Trend> trends = aggregateIntoTrends(tweets, places);

        return new TrendsList(trends.size(), DateTime.now(), timeFrame, trends);
    }

    private Callable<Optional<PlaceDetails>> createFindPlaceTask(final Tweet tweet) {
        return new BusinessNameFinderTask(
                SimpleTokenizer.INSTANCE, nameFinderModel, mapsAPIWrapper, tweet);
    }

    private List<Trend> aggregateIntoTrends(final List<Tweet> tweets,
                                            final List<Optional<PlaceDetails>> mentionedPlaces) {

        assert tweets.size() == mentionedPlaces.size();

        // Aggregate tweets by mentioned places
        final Map<String, List<Tweet>> tweetsByPlaces = new HashMap<>();
        for (int i = 0; i < tweets.size(); i++) {
            final Tweet tweet = tweets.get(i);
            final Optional<PlaceDetails> placeDetails = mentionedPlaces.get(i);
            placeDetails.ifPresent(details -> {
                if (!tweetsByPlaces.containsKey(details.placeId)) {
                    tweetsByPlaces.put(details.placeId, new ArrayList<>() {{
                        add(tweet);
                    }});
                }
                else {
                    tweetsByPlaces.get(details.placeId).add(tweet);
                }
            });
        }
        // Reference places by placeId
        final Map<String, PlaceDetails> placesById = mentionedPlaces.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        placeDetail -> placeDetail.placeId, Function.identity(), (curr, next) -> curr));

        // Reduce aggregated tweets into trends in sorted order with respect to
        //  1. Tweets
        //  2. Favourites
        //  3. Retweets
        return tweetsByPlaces.keySet().stream()
                .map(placeId -> {
                    final List<Tweet> tweetsWithPlaceId = tweetsByPlaces.get(placeId);
                    final int totalTweets = tweetsWithPlaceId.size();
                    final int totalRetweets = tweetsWithPlaceId.stream()
                            .map(Tweet::getRetweetCount)
                            .reduce(0, Math::addExact);
                    final int totalFavourites = tweetsWithPlaceId.stream()
                            .map(Tweet::getRetweetCount)
                            .reduce(0, Math::addExact);

                    return new Trend(placesById.get(placeId), totalTweets, totalRetweets, totalFavourites);
                })
                .sorted()
                .collect(Collectors.toList());
    }
}
