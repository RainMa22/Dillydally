package me.rainma22.dillydally.sslcert.certificategetter;

import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.rainma22.dillydally.conf.ConfBean;
import me.rainma22.dillydally.exceptions.FailedToContactServerException;
import me.rainma22.dillydally.exceptions.InvalidJSONResponseException;
import me.rainma22.dillydally.sslcert.ResponseConstants;
import me.rainma22.dillydally.sslcert.certificategetter.acme.ACMEAccount;
import me.rainma22.dillydally.sslcert.certificategetter.acme.ACMEClient;
import me.rainma22.dillydally.sslcert.certificategetter.acme.ACMEOrder;
import me.rainma22.dillydally.sslcert.challengecompletion.ChallengeCompletor;
import me.rainma22.dillydally.sslcert.challengecompletion.ChallengeCompletors;
import me.rainma22.dillydally.util.GenUtils;

/**
 * ACME-supporting certificate getter with state machine design
 */
class ACMECertificateGetter2 implements CertificateGetterInterface {
        private static final Logger logger = LogManager.getLogger();
        private KeyPair acmeKeyPair;
        private ConfBean conf;

        private KeyPair sslKeyPair = null;
        private X509Certificate[] certs = null;
        private ACMEClient acmeClient = null;

        protected ACMECertificateGetter2() {
        }

        @Override
        public void setConf(ConfBean conf) {
                this.conf = conf;
        }

        @Override
        public void setACMEKeyPair(KeyPair kp) {
                acmeKeyPair = kp;
        }

        @Override
        public KeyPair getACMEKeyPair() {
                return acmeKeyPair;
        }

        @Override
        public void setCert(KeyPair sslKeyPair, X509Certificate[] certs) {
                this.sslKeyPair = sslKeyPair;
                this.certs = certs;
        }

        // TOOO: stub
        @Override
        public Pair<KeyPair, X509Certificate[]> getCert() {
                boolean offline = false;
                try {
                        acmeClient = acmeClient != null ? acmeClient
                                        : ACMEClient.from(URI.create(conf.getServerUrl()), conf);
                } catch (FailedToContactServerException | InvalidJSONResponseException e) {
                        logger.warn("Failed to contact server");
                        logger.warn("Offine mode is ON");
                        logger.warn(e);
                        offline = true;
                }
                if (offline) {
                        return Pair.of(sslKeyPair, certs);
                } else {
                        ACMEAccount account;
                        ACMEOrder order;
                        List<AutoCloseable> cleanups = List.of();
                        try {
                                if (certs != null && !acmeClient.shouldRenew(certs)) {
                                        return Pair.of(sslKeyPair, certs);
                                }
                                if (sslKeyPair == null) {
                                        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                                        gen.initialize(2048);
                                        sslKeyPair = gen.genKeyPair();
                                }
                                account = acmeClient.registerAccount(acmeKeyPair);
                                order = acmeClient.createOrder(account, certs);
                                ChallengeCompletor completor = ChallengeCompletors.fromConf(conf);

                                cleanups = order.authzURLs().stream()
                                                .map(authzStr -> {
                                                        try {
                                                                return acmeClient.getAuthz(account, authzStr);
                                                        } catch (FailedToContactServerException
                                                                        | InvalidJSONResponseException e) {
                                                                throw new RuntimeException(e);
                                                        }
                                                })
                                                .map(authz -> authz.challenge().stream()
                                                                .filter(chal -> chal.getType()
                                                                                .equalsIgnoreCase("http-01"))
                                                                .findAny()
                                                                .map(chal -> {
                                                                        try {
                                                                                AutoCloseable res = completor
                                                                                                .completeChallenge(
                                                                                                                chal,
                                                                                                                acmeKeyPair);
                                                                                acmeClient.submitChallenge(account,
                                                                                                chal.getUrl());
                                                                                return res;
                                                                        } catch (IOException
                                                                                        | FailedToContactServerException
                                                                                        | InvalidJSONResponseException e) {
                                                                                throw new RuntimeException(e);
                                                                        }
                                                                })
                                                                .orElseThrow(() -> new NoSuchElementException(
                                                                                "No http-01 challenge found")))
                                                .toList();

                                while (!ResponseConstants.READY
                                                .equals((order = acmeClient.refreshOrder(account, order)).status())) {
                                        logger.always().log("waiting for order to be ready: current order: {}",
                                                        order.status());
                                        Thread.sleep(Duration.ofSeconds(1));
                                }
                                var finalizeOrder = acmeClient.sendFinalize(account, order,
                                                GenUtils.genCSR(conf, sslKeyPair));
                                order = finalizeOrder.getLeft();
                                Duration waitDuration = finalizeOrder.getRight().orElse(Duration.ZERO);
                                Thread.sleep(waitDuration);
                                certs = acmeClient.getCertificateChain(account, order);
                        } catch (Exception e) {
                                logger.warn("Failed to contact server");
                                logger.warn(e);
                                logger.warn("returning stored sslKeyPair and cert");
                        } finally {
                                cleanups.forEach(c -> {
                                        try {
                                                c.close();
                                        } catch (Exception e) {
                                                logger.warn(e);
                                        }
                                });
                        }
                        return Pair.of(sslKeyPair, certs);
                }
        }

}
