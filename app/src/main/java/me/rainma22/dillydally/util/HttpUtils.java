package me.rainma22.dillydally.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

public class HttpUtils {
    public static final DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("EEE, dd MMM uuuu HH:mm:ss 'GMT'", Locale.US)
            .withZone(ZoneOffset.UTC);

    public static Optional<Duration> parseRetryAfter(String RetryAfterString) {
        if (RetryAfterString == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Duration.ofMillis(Long.parseLong(RetryAfterString)));
        } catch (NumberFormatException nfe) {
            try {
                var now = Instant.now();
                var date = Instant.from(HTTP_DATE_FORMATTER.parse(RetryAfterString));
                return now.isBefore(date) ? Optional.of(Duration.between(now, date)) : Optional.empty();
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
    }
}
