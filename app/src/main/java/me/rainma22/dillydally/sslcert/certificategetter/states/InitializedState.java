package me.rainma22.dillydally.sslcert.certificategetter.states;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;

import io.jsonwebtoken.security.Jwks;
import me.rainma22.dillydally.builder.JoseHttpRequest;
import me.rainma22.dillydally.exceptions.DillyDallyRuntimeException;
import me.rainma22.dillydally.sslcert.ACMEJWS;
import me.rainma22.dillydally.sslcert.ResourceLocationResponse;
import me.rainma22.dillydally.sslcert.certificategetter.CertificateGetterContext;

public class InitializedState implements CertificateGetterState {

    @Override
    public void handle(CertificateGetterContext ctx) {
        try {
            var kp = ctx.getAcmeKeyPair();
            ResourceLocationResponse resourceLocations = ctx.getResourceLocations();
            // send new account request
            if (resourceLocations.getMeta().isExternalAccountRequired()) {
                throw new UnsupportedOperationException("External Account not supported");
            }
            var client = ctx.getClient();
            var jwk = Jwks.builder().key(kp.getPublic()).build();
            var jws = ACMEJWS.withJWK(jwk, client.nextNonce(), resourceLocations.getNewAccount(), kp.getPrivate());
            jws.content(new JSONObject(
                    Map.of("termsOfServiceAgreed", true)).toString());
            var reqBody = ACMEJWS.toString(jws);
            var req = JoseHttpRequest.newBuilder(URI.create(resourceLocations.getNewAccount()))
                    .POST(HttpRequest.BodyPublishers.ofString(reqBody))
                    .build();
            var res = client.send(req, BodyHandlers.ofString());
            var accountLocation = res.headers().firstValue("Location").get();
            ctx.setAccountLocation(accountLocation);
        } catch (Exception e) {
            LogManager.getLogger().warn(e);
            ctx.updateError(e);
        }
    }

}
