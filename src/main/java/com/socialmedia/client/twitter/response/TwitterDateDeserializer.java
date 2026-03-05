package com.socialmedia.client.twitter.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

/**
 * Custom deserializer for Twitter date format.
 * Handles formats like: "Mon Nov 20 11:53:28 +0000 2017"
 */
public class TwitterDateDeserializer extends JsonDeserializer<Instant> {

    // Twitter date format: "EEE MMM dd HH:mm:ss Z yyyy"
    // Example: "Mon Nov 20 11:53:28 +0000 2017"
    private static final DateTimeFormatter TWITTER_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("EEE MMM dd HH:mm:ss Z yyyy")
            .toFormatter(Locale.ENGLISH);

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText();
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        try {
            // Try Twitter date format first
            return java.time.ZonedDateTime.parse(dateString, TWITTER_DATE_FORMATTER)
                    .toInstant();
        } catch (Exception e) {
            try {
                // Try ISO-8601 format
                return Instant.parse(dateString);
            } catch (Exception e2) {
                // Try other common formats
                try {
                    // Try format without timezone offset
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
                    return java.time.LocalDateTime.parse(dateString, formatter)
                            .atZone(ZoneOffset.UTC)
                            .toInstant();
                } catch (Exception e3) {
                    throw new IOException("Unable to parse date: " + dateString, e3);
                }
            }
        }
    }
}

