package me.rainma22.dillydally.sslcert.certificategetter.acme;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;

import io.jsonwebtoken.security.Jwks;
import me.rainma22.dillydally.sslcert.ACMEHttpClient;
import me.rainma22.dillydally.sslcert.ACMEJWS;
import me.rainma22.dillydally.sslcert.AuthChallengeResponse;
import me.rainma22.dillydally.sslcert.NewOrderResponse;
import me.rainma22.dillydally.sslcert.OrderChallengeBean;
import me.rainma22.dillydally.sslcert.OrderIdentifierBean;
import me.rainma22.dillydally.sslcert.RenewalInfoResponse;
import me.rainma22.dillydally.sslcert.ResourceLocationResponse;
import me.rainma22.dillydally.sslcert.ResponseConstants;
import me.rainma22.dillydally.util.HttpStatus;
import me.rainma22.dillydally.util.HttpUtils;
import me.rainma22.dillydally.builder.DillyDallyHttpClient;
import me.rainma22.dillydally.builder.JoseHttpRequest;
import me.rainma22.dillydally.conf.ConfBean;
import me.rainma22.dillydally.exceptions.BadResourceLocationException;
import me.rainma22.dillydally.exceptions.FailedToContactServerException;
import me.rainma22.dillydally.exceptions.InvalidJSONResponseException;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import static me.rainma22.dillydally.util.NullGuard.mustBeNotNull;

public class ACMEClient {
    private ResourceLocationResponse resLoc;
    private ACMEHttpClient acmeHttpClient;
    private ConfBean conf;

    private ACMEClient(ConfBean conf, ResourceLocationResponse resLoc) {
        this.resLoc = resLoc;
        this.conf = conf;
        acmeHttpClient = new ACMEHttpClient(conf, resLoc);
    }

    private String getNextNonce() throws FailedToContactServerException {
        try {
            return acmeHttpClient.nextNonce();
        } catch (IOException | InterruptedException e) {
            throw new FailedToContactServerException("Failed to get new nonce from server", e);
        }
    }

    private Optional<TemporalAccessor> parseTimeIfExists(String str) {
        return parseTimeIfExists(Optional.ofNullable(str));
    }

    private Optional<TemporalAccessor> parseTimeIfExists(Optional<String> str) {
        mustBeNotNull(str, "Optional of String 'str'");
        return str.map(DateTimeFormatter.ISO_DATE_TIME::parse);
    }

    public String serializeCertificate(X509Certificate cert) {
        mustBeNotNull(cert, "certificate");
        final Encoder base64url = Base64.getUrlEncoder().withoutPadding();
        byte[] serial = cert.getSerialNumber().toByteArray();
        String akiBase64 = base64url.encodeToString(cert.getExtensionValue("2.5.29.35"));
        String serialBase64 = base64url.encodeToString(serial);
        String certId = akiBase64 + "." + serialBase64;
        return certId;
    }

    public ACMEAccount registerAccount(KeyPair sslKey) throws FailedToContactServerException {
        mustBeNotNull(sslKey, "SSLKey");
        if (resLoc.getMeta().isExternalAccountRequired()) {
            throw new UnsupportedOperationException("External Account not supported");
        }
        String nonce = getNextNonce();
        String newAcctLoc = resLoc.getNewAccount();

        var jwk = Jwks.builder().key(sslKey.getPublic()).build();
        var jws = ACMEJWS.withJWK(jwk, nonce, newAcctLoc, sslKey.getPrivate());
        jws.content(new JSONObject(
                Map.of("termsOfServiceAgreed", true)).toString());
        var reqBody = ACMEJWS.toString(jws);
        try {
            var req = JoseHttpRequest.newBuilder(URI.create(newAcctLoc))
                    .POST(HttpRequest.BodyPublishers.ofString(reqBody))
                    .build();
            var res = acmeHttpClient.send(req, BodyHandlers.ofString());
            if (!HttpStatus.isSuccess(res.statusCode())) {
                throw new IOException("Bad response: [status %d] %s"
                        .formatted(res.statusCode(), res.body()));
            }
            var accountLocation = res.headers().firstValue("Location").get();
            return new ACMEAccount(accountLocation, sslKey);
        } catch (IOException | InterruptedException e) {
            throw new FailedToContactServerException("Failed ot send to new account endpoint", e);
        } catch (IllegalArgumentException e) {
            throw new FailedToContactServerException("failed to create a valid HTTP Request", e);
        }
    }

    public ACMEOrder createOrder(ACMEAccount account, X509Certificate[] replaceCertificate)
            throws FailedToContactServerException {
        mustBeNotNull(account, "account");
        Map<String, Object> payloadMap = new HashMap<>();
        if (resLoc.isARISupported() && replaceCertificate != null) {
            X509Certificate[] certChain = replaceCertificate;
            payloadMap.put("replaces", serializeCertificate(certChain[0]));
        }
        String nonce = getNextNonce();
        String newOrderURL = resLoc.getNewOrder();
        var jws = ACMEJWS.withAccountLocation(account.location(), nonce, newOrderURL,
                account.keyPair().getPrivate());
        var identifiers = conf.getDomains()
                .stream()
                .map(str -> {
                    var res = new OrderIdentifierBean();
                    res.setType("dns");
                    res.setValue(str);
                    return res;
                }).toList();
        payloadMap.put("identifiers", identifiers);
        var payload = new JSONObject(payloadMap);
        jws.content(payload.toString());
        var reqBody = ACMEJWS.toString(jws);
        var req = JoseHttpRequest.newBuilder(URI.create(resLoc.getNewOrder()))
                .POST(BodyPublishers.ofString(reqBody))
                .build();
        try {
            var res = acmeHttpClient.send(req,
                    BodyHandlers.ofString());
            if (HttpStatus.isSuccess(res.statusCode())) {
                var orderLocation = res.headers().firstValue("Location").get();
                var newOrderRes = JSONObject.fromJson(res.body(), NewOrderResponse.class);
                return new ACMEOrder(orderLocation,
                        newOrderRes.getStatus(),
                        parseTimeIfExists(newOrderRes.getExpires()),
                        parseTimeIfExists(newOrderRes.getNotBefore()),
                        parseTimeIfExists(newOrderRes.getNotAfter()),
                        newOrderRes.getIdentifiers(),
                        newOrderRes.getAuthorizations(),
                        newOrderRes.getFinalize(),
                        Optional.ofNullable(newOrderRes.getCertificate()));
            } else {
                throw new IOException("Bad Order response: [status %d] %s".formatted(res.statusCode(), res.body()));
            }
        } catch (IOException | InterruptedException e) {
            throw new FailedToContactServerException("Failed to send to new order endpoint", e);
        }
    }

    public ACMEAuthz getAuthz(ACMEAccount account, String authzString)
            throws FailedToContactServerException, InvalidJSONResponseException {
        mustBeNotNull(account, "account");
        mustBeNotNull(authzString, "authorization location");
        var jws = ACMEJWS.withAccountLocation(account.location(), getNextNonce(), authzString,
                account.keyPair().getPrivate());
        HttpResponse<String> res = null;
        try {
            HttpRequest req = JoseHttpRequest.newBuilder(URI.create(authzString))
                    .POST(BodyPublishers.ofString(ACMEJWS.toString(jws)))
                    .build();
            res = acmeHttpClient.send(req, BodyHandlers.ofString());
            if (HttpStatus.isSuccess(res.statusCode())) {
                var authChallRes = JSONObject.fromJson(res.body(), AuthChallengeResponse.class);
                return new ACMEAuthz(authChallRes.getStatus(),
                        parseTimeIfExists(authChallRes.getExpires()),
                        authChallRes.getIdentifier(),
                        authChallRes.getChallenges());
            } else {
                throw new IOException("Bad Reponse [status %d] %s"
                        .formatted(res.statusCode(), res.body()));
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("bad authzString", e);
        } catch (IOException | InterruptedException e) {
            throw new FailedToContactServerException("Failed to connect to authz endpoint: " + authzString, e);
        } catch (JSONException | ClassCastException e) {
            throw new InvalidJSONResponseException("bad Authz Response: " + res == null ? null : res.body(), e);
        }
    }

    public OrderChallengeBean submitChallenge(ACMEAccount account, String challengeUrl)
            throws FailedToContactServerException, InvalidJSONResponseException {
        mustBeNotNull(account, "account");
        mustBeNotNull(challengeUrl, "challenge url");
        var accountLocation = account.location();
        var kp = account.keyPair();
        URI challengeUri = URI.create(challengeUrl);
        HttpResponse<String> res = null;
        try {
            var jws = ACMEJWS.withAccountLocation(accountLocation, getNextNonce(), challengeUrl,
                    kp.getPrivate());
            jws.content("{}");
            var req = JoseHttpRequest.newBuilder(challengeUri)
                    .POST(BodyPublishers.ofString(ACMEJWS.toString(jws)))
                    .build();
            res = acmeHttpClient.send(req, BodyHandlers.ofString());
            if (!HttpStatus.isSuccess(res.statusCode())) {
                throw new IOException("Bad response: [status %d] %s"
                        .formatted(res.statusCode(), res.body()));
            }
            JSONObject obj = new JSONObject(res.body());
            return obj.fromJson(OrderChallengeBean.class);
        } catch (IOException | InterruptedException e) {
            throw new FailedToContactServerException("failed to access " + challengeUrl, e);
        } catch (JSONException | ClassCastException e) {
            throw new InvalidJSONResponseException(
                    "bad challenge submission Response: " + res == null ? null : res.body(),
                    e);
        }
    }

    public ACMEOrder refreshOrder(ACMEAccount account, ACMEOrder order)
            throws FailedToContactServerException, InvalidJSONResponseException {
        mustBeNotNull(account, "account");
        mustBeNotNull(order, "order");
        URI orderUri;
        try {
            orderUri = URI.create(order.location());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("order.location() is not a proper URI", e);
        }
        var jws = ACMEJWS.withAccountLocation(account.location(), getNextNonce(),
                order.location(), account.keyPair().getPrivate());
        var req = JoseHttpRequest.newBuilder(orderUri)
                .POST(BodyPublishers.ofString(ACMEJWS.toString(jws)))
                .build();
        HttpResponse<String> res = null;
        try {
            res = acmeHttpClient.send(req, BodyHandlers.ofString());
            if (HttpStatus.isSuccess(res.statusCode())) {
                var orderLocation = order.location();
                System.out.println(new JSONObject(res.body()).toString(2));
                var newOrderRes = JSONObject.fromJson(res.body(), NewOrderResponse.class);
                return new ACMEOrder(orderLocation,
                        newOrderRes.getStatus(),
                        parseTimeIfExists(newOrderRes.getExpires()),
                        parseTimeIfExists(newOrderRes.getNotBefore()),
                        parseTimeIfExists(newOrderRes.getNotAfter()),
                        newOrderRes.getIdentifiers(),
                        newOrderRes.getAuthorizations(),
                        newOrderRes.getFinalize(),
                        Optional.ofNullable(newOrderRes.getCertificate()));
            } else {
                throw new IOException("Bad Order response: [status %d] %s".formatted(res.statusCode(), res.body()));
            }
        } catch (IOException | InterruptedException e) {
            throw new FailedToContactServerException("Failed to send to new order endpoint", e);
        }
    }

    public String getNewStatus(ACMEAccount account, ACMEOrder order)
            throws FailedToContactServerException, InvalidJSONResponseException {
        mustBeNotNull(account, "account");
        mustBeNotNull(order, "order");
        return refreshOrder(account, order).status();
    }

    public boolean isOrderReady(ACMEAccount account, ACMEOrder order) throws FailedToContactServerException {
        mustBeNotNull(account, "account");
        mustBeNotNull(order, "order");
        if (ResponseConstants.READY.equals(order.status())) {
            return true;
        }
        URI orderUri;
        try {
            orderUri = URI.create(order.location());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("order.location() is not a proper URI", e);
        }
        var jws = ACMEJWS.withAccountLocation(account.location(), getNextNonce(),
                order.location(), account.keyPair().getPrivate());
        var req = JoseHttpRequest.newBuilder(orderUri)
                .POST(BodyPublishers.ofString(ACMEJWS.toString(jws)))
                .build();
        HttpResponse<String> res;
        try {
            res = acmeHttpClient.send(req, BodyHandlers.ofString());
            if (HttpStatus.isSuccess(res.statusCode())) {
                throw new IOException("Invalid Response [status: %d] %s"
                        .formatted(res.statusCode(), res.body()));
            }

        } catch (IOException | InterruptedException e) {
            throw new FailedToContactServerException("unable to connect to " + orderUri.toString(), e);
        }
        JSONObject obj = new JSONObject(res.body());
        String status = obj.getString("status");
        return ResponseConstants.READY.equals(status);
    }

    public Pair<ACMEOrder, Optional<Duration>> sendFinalize(ACMEAccount account, ACMEOrder order,
            PKCS10CertificationRequest csr)
            throws OrderNotReadyException, FailedToContactServerException {
        mustBeNotNull(account, "account");
        mustBeNotNull(order, "order");
        mustBeNotNull(csr, "csr");
        if (!isOrderReady(account, order)) {
            throw new OrderNotReadyException("Order is not ready yet!");
        }
        var accountLocation = account.location();
        var client = acmeHttpClient;
        var kp = account.keyPair();
        Encoder Base64Url = Base64.getUrlEncoder().withoutPadding();
        var finalizeUrl = order.finalizeURL();
        try {
            URI finalizeUri = URI.create(finalizeUrl);
            var jws = ACMEJWS.withAccountLocation(accountLocation, client.nextNonce(), finalizeUrl, kp.getPrivate());
            jws.content(new JSONObject(
                    Map.of("CSR", Base64Url.encodeToString(csr.getEncoded())))
                    .toString());
            var req = JoseHttpRequest.newBuilder(finalizeUri)
                    .POST(BodyPublishers.ofString(ACMEJWS.toString(jws)))
                    .build();
            var res = client.send(req, BodyHandlers.ofString());
            if (!HttpStatus.isSuccess(res.statusCode()))
                throw new IOException("Invalid Response [status: %d] %s"
                        .formatted(res.statusCode(), res.body()));
            var orderLocation = order.location();

            var newOrderRes = JSONObject.fromJson(res.body(), NewOrderResponse.class);
            var order1 = new ACMEOrder(orderLocation,
                    newOrderRes.getStatus(),
                    parseTimeIfExists(newOrderRes.getExpires()),
                    parseTimeIfExists(newOrderRes.getNotBefore()),
                    parseTimeIfExists(newOrderRes.getNotAfter()),
                    newOrderRes.getIdentifiers(),
                    newOrderRes.getAuthorizations(),
                    newOrderRes.getFinalize(),
                    Optional.ofNullable(newOrderRes.getCertificate()));
            if (ResponseConstants.PROCESSING.equals(order1.status())) {
                return Pair.of(order1,
                        res.headers().firstValue("Retry-After")
                                .flatMap(HttpUtils::parseRetryAfter));
            } else
                return Pair.of(order1, Optional.empty());

        } catch (IOException | InterruptedException e) {
            throw new FailedToContactServerException("unable to connect to finalize URL: " + finalizeUrl.toString(), e);
        }
    }

    public boolean shouldRenew(X509Certificate[] certChain) throws FailedToContactServerException {
        mustBeNotNull(certChain);
        if (resLoc.isARISupported()) {
            final Encoder base64url = Base64.getUrlEncoder().withoutPadding();
            byte[] serial = certChain[0].getSerialNumber().toByteArray();
            // get authorityKeyIdentifier
            String akiBase64 = base64url.encodeToString(certChain[0].getExtensionValue("2.5.29.35"));
            String serialBase64 = base64url.encodeToString(serial);
            String certId = akiBase64 + "." + serialBase64;
            HttpRequest req = HttpRequest
                    .newBuilder(URI.create(resLoc.getRenewalInfo() + "/").resolve("/" + certId))
                    .GET()
                    .build();
            HttpResponse<String> res;
            try {
                res = acmeHttpClient.send(req, BodyHandlers.ofString());
                if (!HttpStatus.isSuccess(res.statusCode())) {
                    throw new IOException("Invalid Response: [status %d] %s".formatted(res.statusCode(), res.body()));
                }
            } catch (IOException | InterruptedException e) {
                throw new FailedToContactServerException("Failed to contact server for renewalInformation", e);
            }
            RenewalInfoResponse renewalInfo = JSONObject.fromJson(res.body(), RenewalInfoResponse.class);
            var suggestedWindow = renewalInfo.getSuggestedWindow();
            LocalDateTime notBefore = LocalDateTime
                    .from(DateTimeFormatter.ISO_DATE_TIME.parse(suggestedWindow.getNotBefore()));
            LocalDateTime notAfter = LocalDateTime
                    .from(DateTimeFormatter.ISO_DATE_TIME.parse(suggestedWindow.getNotAfter()));
            return notBefore.isBefore(LocalDateTime.now()) || notAfter.isBefore(LocalDateTime.now());
        } else {
            Date notAfter = certChain[0].getNotAfter();
            Date now = Date.from(Instant.now());
            return (notAfter.getTime() - now.getTime() < Duration
                    .ofDays(conf.getSslCertificateConf().getRenewalThresholdInDays())
                    .toMillis());
        }
    }

    public X509Certificate[] getCertificateChain(ACMEAccount account, ACMEOrder order)
            throws OrderNotReadyException, FailedToContactServerException, InvalidJSONResponseException {
        mustBeNotNull(account, "account");
        mustBeNotNull(order, "order");
        String status = order.status();
        if (!ResponseConstants.VALID.equalsIgnoreCase(status) &&
                !ResponseConstants.VALID.equalsIgnoreCase((status = getNewStatus(account, order)))) {
            throw new OrderNotReadyException("Order is not valid for getting certificate yet!" +
                    " Current status: %s".formatted(status));
        }
        String certUrl = refreshOrder(account, order).certificateURL().orElseThrow();
        var jws = ACMEJWS.withAccountLocation(account.location(), getNextNonce(), certUrl, 
        account.keyPair().getPrivate());
        var req = JoseHttpRequest.newBuilder(URI.create(certUrl))
                .POST(BodyPublishers.ofString(ACMEJWS.toString(jws)))
                .build();
        List<X509Certificate> certs = new ArrayList<>();
        try (PEMParser parser = new PEMParser(
                new StringReader(acmeHttpClient.send(req, BodyHandlers.ofString()).body()))) {
            JcaX509CertificateConverter converter = new JcaX509CertificateConverter()
                    .setProvider(new BouncyCastleProvider());
            Object nextPem;
            while ((nextPem = parser.readObject()) != null) {
                certs.add(converter.getCertificate((X509CertificateHolder) nextPem));
            }
        } catch (IOException | InterruptedException | CertificateException e) {
            throw new InvalidJSONResponseException(e);
        }
        return certs.stream().toArray(X509Certificate[]::new);
    }

    public static ACMEClient from(URI directoryURI, ConfBean conf)
            throws FailedToContactServerException, InvalidJSONResponseException {
        HttpClient client = DillyDallyHttpClient.newBuilder(Optional.of(conf))
                .build();
        var req = HttpRequest.newBuilder(directoryURI)
                .GET()
                .build();
        HttpResponse<String> res = null;
        ResourceLocationResponse resLoc;
        try {
            res = client.send(req, BodyHandlers.ofString());
            if (!HttpStatus.isSuccess(res.statusCode()))
                throw new IOException("Bad response when contacting server: [status: %d] body: %s"
                        .formatted(res.statusCode(), res.body()));

            resLoc = JSONObject.fromJson(res.body(), ResourceLocationResponse.class);
        } catch (IOException | InterruptedException e) {
            throw new FailedToContactServerException("Failed to contact server: " + directoryURI.toString(), e);
        } catch (JSONException | ClassCastException e2) {
            throw new BadResourceLocationException("Bad Resource Location String: " + res == null ? null : res.body());
        }
        return new ACMEClient(conf, resLoc);
    }

}
