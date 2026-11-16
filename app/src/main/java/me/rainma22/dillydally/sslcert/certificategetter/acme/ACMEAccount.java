package me.rainma22.dillydally.sslcert.certificategetter.acme;

import java.security.KeyPair;

/**
 * ACMEAccount
 */
public record ACMEAccount(String location, KeyPair keyPair) {
}
