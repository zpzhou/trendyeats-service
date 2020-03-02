package com.zpzhou.trendingrestaurants.handlers;

import com.google.maps.ImageResult;
import com.zpzhou.trendingrestaurants.search.places.GoogleMapsAPIWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

@RequiredArgsConstructor
public class PhotoHandler {

    private static final Logger logger = LogManager.getLogger();

    private final GoogleMapsAPIWrapper mapsAPIWrapper;

    public Optional<ImageResult> getPhoto(final String photoReference,
                                          final Optional<Integer> maxWidth,
                                          final Optional<Integer> maxHeight) {

        return mapsAPIWrapper.getPlacePhoto(photoReference, maxWidth, maxHeight);
    }
}
