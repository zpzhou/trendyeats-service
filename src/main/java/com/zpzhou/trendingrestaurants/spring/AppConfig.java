package com.zpzhou.trendingrestaurants.spring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.zpzhou.trendingrestaurants.controllers.PhotoController;
import com.zpzhou.trendingrestaurants.controllers.PingController;
import com.zpzhou.trendingrestaurants.controllers.TrendsController;
import com.zpzhou.trendingrestaurants.controllers.cache.TumblingCache;
import com.zpzhou.trendingrestaurants.handlers.PhotoHandler;
import com.zpzhou.trendingrestaurants.handlers.TrendsHandler;
import com.zpzhou.trendingrestaurants.model.TrendsList;
import com.zpzhou.trendingrestaurants.search.places.GoogleMapsAPIWrapper;
import com.zpzhou.trendingrestaurants.search.tweets.TwitterSearchAPIClient;
import com.zpzhou.trendingrestaurants.search.tweets.TwitterSearchQueryResponse;
import com.zpzhou.trendingrestaurants.search.tweets.TwitterSearchResponseDeserializer;
import okhttp3.OkHttpClient;
import opennlp.tools.namefind.TokenNameFinderModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Configuration
public class AppConfig {

    private static final Logger logger = LogManager.getLogger();

    @Value("${twitter.search.url}")
    public String twitterSearchURL;

    @Value("${twitter.bearer.access.token}")
    private String twitterBearerAccessToken;

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    @Value("#{new Integer(${thread.pool.size})}")
    private int threadPoolSize;

    @Value("#{new Integer(${tumbling.window.minutes})}")
    private int tumblingWindowMinutes;

    @Value("#{new Integer(${tumbling.window.capacity})}")
    private int tumblingWindowCapacity;

    @Bean
    @Scope(value = "singleton")
    public TwitterSearchAPIClient twitterSearchAPIClient () {
        logger.info("twitter search URL: {}", twitterSearchURL);
        final OkHttpClient httpClient = new OkHttpClient();
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(TwitterSearchQueryResponse.class, new TwitterSearchResponseDeserializer())
                .create();
        return new TwitterSearchAPIClient(httpClient, twitterBearerAccessToken, twitterSearchURL, gson);
    }

    @Bean
    @Scope(value = "singleton")
    public GoogleMapsAPIWrapper googleMapsAPIWrapper() {
        final GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(googleMapsApiKey)
                .maxRetries(3)
                .readTimeout(2000, TimeUnit.MILLISECONDS)
                .build();
        return new GoogleMapsAPIWrapper(context);
    }

    @Bean
    @Scope(value = "singleton")
    public TrendsHandler trendsHandler() throws IOException {
        try (final InputStream inputStream = new ClassPathResource("static/food_keywords.txt")
                .getInputStream())
        {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            final Set<String> keywords = reader.lines()
                    .collect(Collectors.toSet());
            logger.info("Food keywords: {}", keywords);

            return new TrendsHandler(twitterSearchAPIClient(), googleMapsAPIWrapper(),
                    executorService(), nameFinderModel(), keywords);
        }
    }

    @Bean
    @Scope(value = "singleton")
    public PhotoHandler photoHandler() {
        return new PhotoHandler(googleMapsAPIWrapper());
    }

    public TokenNameFinderModel nameFinderModel() throws IOException {
        try(final InputStream inputStream = new ClassPathResource("static/en-ner-organization.bin")
                .getInputStream())
        {
            return new TokenNameFinderModel(inputStream);
        }
    }


    private Set<String> loadKeyWordFileFromPath(final String path) throws IOException {
        final InputStream inputStream = new ClassPathResource(path)
                .getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines()
                .collect(Collectors.toSet());
    }

    @Bean
    @Scope(value = "singleton")
    public TrendsController trendsController() throws IOException {
        return new TrendsController(trendsHandler(), tumblingCache());
    }

    @Bean
    @Scope(value = "singleton")
    public PhotoController photoController() {
        return new PhotoController(photoHandler());
    }

    @Bean
    @Scope(value = "singleton")
    public PingController pingController() {
        return new PingController();
    }

    @Bean
    @Scope(value = "singleton")
    public TumblingCache<String, TrendsList> tumblingCache() {
        return new TumblingCache<>(
                scheduledExecutorService(),
                new ReentrantReadWriteLock(),
                new HashMap<>(),
                tumblingWindowMinutes,
                tumblingWindowCapacity);
    }

    @Bean(destroyMethod = "shutdown")
    @Scope(value = "singleton")
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    @Bean(destroyMethod = "shutdown")
    @Scope(value = "singleton")
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
