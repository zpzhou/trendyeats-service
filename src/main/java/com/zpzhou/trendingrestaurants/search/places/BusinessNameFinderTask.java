package com.zpzhou.trendingrestaurants.search.places;

import com.google.maps.model.AddressType;
import com.google.maps.model.PlaceDetails;
import com.zpzhou.trendingrestaurants.model.Tweet;
import lombok.RequiredArgsConstructor;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

@RequiredArgsConstructor
public class BusinessNameFinderTask implements Callable<Optional<PlaceDetails>> {

    private static final Logger logger = LogManager.getLogger();

    private static Set<AddressType> BUSINESS_TYPES = EnumSet.of(
            AddressType.CAFE,
            AddressType.BAKERY,
            AddressType.RESTAURANT,
            AddressType.BAR,
            AddressType.FOOD,
            AddressType.GROCERY_OR_SUPERMARKET);

    private final SimpleTokenizer tokenizer;
    private final TokenNameFinderModel nameFinderModel;
    private final GoogleMapsAPIWrapper mapsAPIWrapper;
    private final Tweet tweet;

    public Optional<PlaceDetails> call() {
        // Attempt to find business from LatLng if available
        return tweet.getLatLng().flatMap(latLng -> mapsAPIWrapper
                .getReverseGeoCoding(latLng.getLat(), latLng.getLng())
                .stream()
                .findFirst()
                .flatMap(result -> mapsAPIWrapper.getPlaceDetails(result.placeId)))
                .filter(this::isPlaceOfDesiredType)
        // If we can't find a business from the LatLng, tokenize the tweet
        // and attempt to search for businesses by text
                .or(() -> findMostProbableBusiness()
                        .flatMap(business -> mapsAPIWrapper.findPlacesByText(business)
                                .stream()
                                .findFirst()
                                .flatMap(result -> mapsAPIWrapper.getPlaceDetails(result.placeId)))
                                .filter(this::isPlaceOfDesiredType));

    }

    private Optional<String> findMostProbableBusiness() {
        final NameFinderME nameFinder = new NameFinderME(nameFinderModel);
        final String[] tokens = tokenizer.tokenize(tweet.getText());
        final Span[] spans = nameFinder.find(tokens);
        nameFinder.clearAdaptiveData();

        int maxProbableBusinessIdx = -1;
        double maxProb = 0.0;
        for (int i = 0; i < spans.length; i++) {
            if (spans[i].getProb() > maxProb) {
                maxProb = spans[i].getProb();
                maxProbableBusinessIdx = i;
            }
        }
        if (maxProbableBusinessIdx == -1) {
            return Optional.empty();
        }
        else {
            final Span mostProbableSpan = spans[maxProbableBusinessIdx];
            final String[] tokenizedBusinessName = new String[mostProbableSpan.getEnd() - mostProbableSpan.getStart()];
            for (int i = mostProbableSpan.getStart(); i < mostProbableSpan.getEnd(); i++) {
                tokenizedBusinessName[i - mostProbableSpan.getStart()] = tokens[i];
            }
            return Optional.of(
                    String.join(" ", tokenizedBusinessName));
        }
    }

    private boolean isPlaceOfDesiredType(final PlaceDetails details) {
        return Arrays.asList(details.types).stream()
                .anyMatch(BUSINESS_TYPES::contains);
    }
}
