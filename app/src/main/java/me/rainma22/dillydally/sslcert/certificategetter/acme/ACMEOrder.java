package me.rainma22.dillydally.sslcert.certificategetter.acme;

import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Optional;

import me.rainma22.dillydally.sslcert.OrderIdentifierBean;

/**
 * ACMEOrder
 */
public record ACMEOrder(String location, String status,
        Optional<TemporalAccessor> expires,
        Optional<TemporalAccessor> notBefore,
        Optional<TemporalAccessor> notAfter,
        List<OrderIdentifierBean> identifiers,
        List<String> authzURLs,
        String finalizeURL,
        Optional<String> certificateURL) {


}
