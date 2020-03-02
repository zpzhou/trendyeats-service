package com.zpzhou.trendingrestaurants.controllers;

import com.google.maps.ImageResult;
import com.zpzhou.trendingrestaurants.handlers.PhotoHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Optional;

@RestController
@RequestMapping("/photo")
@RequiredArgsConstructor
public class PhotoController {

    private static final Logger logger = LogManager.getLogger();

    private final PhotoHandler photoHandler;

    @GetMapping(produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getPhoto(
            final @RequestParam("photoreference") String photoReference,
            final @RequestParam(value = "maxwidth", required = false) String maxWidth,
            final @RequestParam(value = "maxheight", required = false) String maxHeight
    ) {
        final Optional<Integer> maxWidthIntValue = parseAndValidateIntegerValue(maxWidth);
        final Optional<Integer> maxHeightIntValue = parseAndValidateIntegerValue(maxHeight);

        final ImageResult result = photoHandler.getPhoto(photoReference, maxWidthIntValue, maxHeightIntValue)
                .orElseThrow(() -> new HttpServerErrorException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong, please try again."));

        return result.imageData;
    }

    private Optional<Integer> parseAndValidateIntegerValue(final String value) {
        try {
            return Optional.ofNullable(value)
                    .map(Integer::valueOf)
                    .filter(intValue -> intValue > 0 && intValue < 1601);
        }
        catch (final NumberFormatException ex) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    "maxwidth and maxheight must be integers.");
        }
    }

}
