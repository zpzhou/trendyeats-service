package com.zpzhou.trendingrestaurants.search.places;

import com.google.maps.FindPlaceFromTextRequest;
import com.google.maps.FindPlaceFromTextRequest.InputType;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.ImageResult;
import com.google.maps.PhotoRequest;
import com.google.maps.PlaceDetailsRequest;
import com.google.maps.PlacesApi;
import com.google.maps.model.FindPlaceFromText;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlacesSearchResult;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class GoogleMapsAPIWrapper {

    private static final Logger logger = LogManager.getLogger();

    private static final FindPlaceFromTextRequest.FieldMask[] FIND_PLACE_FIELD_MASKS = {
            FindPlaceFromTextRequest.FieldMask.PLACE_ID,
    };
    private static final PlaceDetailsRequest.FieldMask[] PLACE_DETAILS_FIELD_MASKS = {
            PlaceDetailsRequest.FieldMask.PLACE_ID,
            PlaceDetailsRequest.FieldMask.NAME,
            PlaceDetailsRequest.FieldMask.FORMATTED_ADDRESS,
            PlaceDetailsRequest.FieldMask.RATING,
            //PlaceDetailsRequest.FieldMask.USER_RATINGS_TOTAL,
            PlaceDetailsRequest.FieldMask.TYPES,
            //PlaceDetailsRequest.FieldMask.GEOMETRY,
            PlaceDetailsRequest.FieldMask.PHOTOS
    };

    private final GeoApiContext context;

    public List<PlacesSearchResult> findPlacesByText(final String text) {
        try {
            final FindPlaceFromTextRequest request = PlacesApi
                    .findPlaceFromText(context, text, InputType.TEXT_QUERY)
                    .fields(FIND_PLACE_FIELD_MASKS);
            final FindPlaceFromText response = request.await();
            return Arrays.asList(response.candidates);
        }
        catch (final Exception ex) {
            logger.warn("Failed to execute {} with input {}: {}",
                    FindPlaceFromTextRequest.class.getSimpleName(), text, ex);
            return Collections.EMPTY_LIST;
        }
    }

    public Optional<PlaceDetails> getPlaceDetails(final String placeId) {
        try {
            final PlaceDetailsRequest request = PlacesApi
                    .placeDetails(context, placeId)
                    .fields(PLACE_DETAILS_FIELD_MASKS);
            return Optional.ofNullable(request.await());
        }
        catch (final Exception ex) {
            logger.warn("Failed to execute {} with input {}: {}",
                    PlaceDetailsRequest.class.getSimpleName(), placeId, ex);
            return Optional.empty();
        }
    }

    public List<GeocodingResult> getReverseGeoCoding(final BigDecimal lat, final BigDecimal lng) {
        try {
            final GeocodingApiRequest request = GeocodingApi
                    .reverseGeocode(context, new LatLng(lat.doubleValue(), lng.doubleValue()));
            return Arrays.asList(request.await());
        }
        catch (final Exception ex) {
            logger.warn("Failed to execute {} with coordinates {}, {}: {}",
                    GeocodingApiRequest.class.getSimpleName(), lat, lng, ex);
            return Collections.EMPTY_LIST;
        }
    }

    public Optional<ImageResult> getPlacePhoto(final String photoReference,
                                               final Optional<Integer> maxWidth,
                                               final Optional<Integer> maxHeight) {
        try {
             final PhotoRequest request = new PhotoRequest(context);
             request.photoReference(photoReference);
             maxWidth.ifPresent(request::maxWidth);
             maxHeight.ifPresent(request::maxHeight);

             // request must contain at least max width OR max width
             if (maxHeight.isEmpty() && maxHeight.isEmpty()) {
                 request.maxWidth(400);
             }

             return Optional.of(request.await());
        }
        catch (final Exception ex) {
            logger.warn("Failed to fetch photo with reference {}: {}", photoReference, ex.getMessage());
            return Optional.empty();
       }
    }
}
