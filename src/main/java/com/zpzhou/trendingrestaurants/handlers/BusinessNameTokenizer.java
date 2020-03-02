package com.zpzhou.trendingrestaurants.handlers;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BusinessNameTokenizer {

    private final List<String> stopWords;

    public List<String> tokenize(final String rawText) {
        // strip text of new lines, punctuation, and hash tags
        final String strippedText = rawText
                .replace("\n", " ")
                .replace(".", " ")
                .replace(",", " ")
                .replace("!", " ")
                .replace("#", "");
        // split text into words separated by spaces
        final List<String> tokens = Arrays.stream(rawText.split("\\s+"))
                .collect(Collectors.toList());

        // output
        final List<String> potentialBusinesses = new ArrayList<>();

        // join adjacent words as they may be included in business names (eg, Greek by Anatoli)
        for (int i = 0; i < tokens.size() - 1; i++) {
            potentialBusinesses.add(String.format("%s %s", tokens.get(i), tokens.get(i+1)));
        }
        for (int i = 0; i < tokens.size() - 2; i++) {
            potentialBusinesses.add(String.format("%s %s %s", tokens.get(i), tokens.get(i+1), tokens.get(i+2)));
        }
        // remove stop words and include remaining words as tokens
        tokens.removeAll(stopWords);
        potentialBusinesses.addAll(tokens);

        return potentialBusinesses;
    }

    public List<String> removeStopWords(final List<String> tokens) {
        final List<String> output = new ArrayList<>(tokens);
        output.removeAll(stopWords);
        return output;
    }
}
