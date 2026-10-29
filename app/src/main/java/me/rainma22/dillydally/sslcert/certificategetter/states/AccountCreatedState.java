package me.rainma22.dillydally.sslcert.certificategetter.states;

import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import me.rainma22.dillydally.exceptions.DillyDallyRuntimeException;
import me.rainma22.dillydally.exceptions.InvalidResponseException;
import me.rainma22.dillydally.sslcert.ACMEJWS;
import me.rainma22.dillydally.sslcert.JoseHttpRequest;
import me.rainma22.dillydally.sslcert.NewOrderResponse;
import me.rainma22.dillydally.sslcert.OrderIdentifier;
import me.rainma22.dillydally.sslcert.certificategetter.CertificateGetterContext;
import me.rainma22.dillydally.util.HttpStatus;

public class AccountCreatedState implements CertificateGetterState {
        private static final Logger logger = LogManager.getLogger();

        @Override
        public void handle(CertificateGetterContext ctx) {
                try {
                        // create order
                        var jws = ACMEJWS.withAccountLocation(ctx.getAccountLocation(), ctx.getClient().nextNonce(),
                                        ctx.getResourceLocations().getNewOrder(),
                                        ctx.getAcmeKeyPair().getPrivate());
                        var identifiers = ctx.getConf().getDomains()
                                        .stream()
                                        .map(str -> {
                                                var res = new OrderIdentifier();
                                                res.setType("dns");
                                                res.setValue(str);
                                                return res;
                                        }).toList();
                        var payload = new JSONObject(
                                        Map.of("identifiers", identifiers));
                        jws.content(payload.toString());
                        var reqBody = ACMEJWS.toString(jws);
                        var req = JoseHttpRequest.newBuilder(URI.create(ctx.getResourceLocations().getNewOrder()))
                                        .POST(BodyPublishers.ofString(reqBody))
                                        .build();
                        var res = ctx.getClient().send(req,
                                        BodyHandlers.ofString());
                        if (HttpStatus.isSuccess(res.statusCode())) {
                                var newOrderResponse = JSONObject.fromJson(res.body(), NewOrderResponse.class);
                                var orderLocation = res.headers().firstValue("Location").get();
                                ctx.setOrderLocation(orderLocation);
                                ctx.setOrderResponse(newOrderResponse);
                                ctx.setOrderExpiry(LocalDateTime
                                                .from(DateTimeFormatter.ISO_DATE_TIME
                                                                .parse(newOrderResponse.getExpires())));
                        } else {
                                throw new InvalidResponseException("bad response: " + res.body());
                        }
                } catch (Exception e) {
                        logger.warn(e);
                        ctx.updateError(e);
                        throw new DillyDallyRuntimeException(e);
                }
        }

}
