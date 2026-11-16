package me.rainma22.dillydally.sslcert.certificategetter.acme;

import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Optional;

import me.rainma22.dillydally.sslcert.OrderChallengeBean;
import me.rainma22.dillydally.sslcert.OrderIdentifierBean;

/**
 * ACMEAuthz
 */
public record ACMEAuthz(String status, 
    Optional<TemporalAccessor> expires, 
    OrderIdentifierBean identifier, 
    List<OrderChallengeBean> challenge) {

}
