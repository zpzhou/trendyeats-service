package com.zpzhou.trendingrestaurants.controllers;

import com.zpzhou.trendingrestaurants.controllers.cache.TumblingCache;
import com.zpzhou.trendingrestaurants.handlers.TrendsHandler;
import com.zpzhou.trendingrestaurants.model.TimeFrame;
import com.zpzhou.trendingrestaurants.model.TrendsList;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/trends")
@RequiredArgsConstructor
public class TrendsController {

    private static final Logger logger = LogManager.getLogger();

    private final TrendsHandler trendsHandler;
    private final TumblingCache<String, TrendsList> cache;

    @GetMapping()
    public TrendsList getTrends(final @RequestParam("place") String place,
                                final @RequestParam("timeframe") String timeFrame) {
        // Validate client input
        final Map<String, TimeFrame> validTimeFrames = Arrays.stream(TimeFrame.values())
                .collect(Collectors.toMap(TimeFrame::toString, Function.identity()));

        if (!validTimeFrames.containsKey(timeFrame)) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    "Invalid timeframe" + timeFrame);
        }
        // Return a cached result if available
        final String cacheKey = buildCacheKey(place, timeFrame);
        logger.info("Received request for: {}", cacheKey); // can use for metrics later

        final Optional<TrendsList> cachedResponse = cache.get(cacheKey);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        // Otherwise, process the request and cache the result
        try {
            final TrendsList trends = trendsHandler.handleGet(place, validTimeFrames.get(timeFrame));
            cache.put(cacheKey, trends);
            return trends;
        }
        catch (final InterruptedException ex) {
            logger.warn("Thread was interrupted {}", ex);
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private String buildCacheKey(final String place, final String timeFrame) {
        return String.format("%s-%s", place, timeFrame);
    }
}
