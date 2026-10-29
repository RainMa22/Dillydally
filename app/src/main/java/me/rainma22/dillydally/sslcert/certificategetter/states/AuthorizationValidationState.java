package me.rainma22.dillydally.sslcert.certificategetter.states;

import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.rainma22.dillydally.sslcert.certificategetter.CertificateGetterContext;

public class AuthorizationValidationState implements CertificateGetterState {
    private static Logger logger = LogManager.getLogger();

    @Override
    public void handle(CertificateGetterContext ctx) {
        try {
            CompletableFuture.allOf(
                    ctx.getCompletedChallenges().stream().toArray(CompletableFuture<?>[]::new))
                    .join();
            ctx.getCompletedChallenges().clear();
        } catch (Exception e) {
            logger.warn(e);
            ctx.updateError(e);
        }
    }

}
