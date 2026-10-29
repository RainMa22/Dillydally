package me.rainma22.dillydally.sslcert.certificategetter.states;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import me.rainma22.dillydally.builder.JoseHttpRequest;
import me.rainma22.dillydally.sslcert.ACMEJWS;
import me.rainma22.dillydally.sslcert.ResponseConstants;
import me.rainma22.dillydally.sslcert.certificategetter.CertificateGetterContext;
import me.rainma22.dillydally.util.GenUtils;

/**
 * FinalizingState
 */
public class RequestCertificateState implements CertificateGetterState {
    private static Logger logger = LogManager.getLogger();

    @Override
    public void handle(CertificateGetterContext ctx) {
        try {
            var conf = ctx.getConf();
            var orderResponse = ctx.getOrderResponse();
            var accountLocation = ctx.getAccountLocation();
            var client = ctx.getClient();
            var kp = ctx.getAcmeKeyPair();
            Encoder Base64Url = Base64.getUrlEncoder().withoutPadding();
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair sslKeyPair = gen.genKeyPair();
            var csr = GenUtils.genCSR(conf, sslKeyPair);
            String finalizeUrl = orderResponse.getFinalize();
            URI finalizeUri = URI.create(finalizeUrl);

            var jws = ACMEJWS.withAccountLocation(accountLocation, client.nextNonce(), finalizeUrl, kp.getPrivate());
            jws.content(new JSONObject(
                    Map.of("CSR", Base64Url.encodeToString(csr.getEncoded())))
                    .toString());
            var req = JoseHttpRequest.newBuilder(finalizeUri)
                    .POST(BodyPublishers.ofString(ACMEJWS.toString(jws)))
                    .build();
            var res = client.send(req, BodyHandlers.ofString());
            JSONObject resJson = new JSONObject(res.body());
            logger.info(resJson);
            String status = resJson.get("status").toString();
            if (ResponseConstants.PROCESSING.equals(status) || ResponseConstants.VALID.equals(status)) {
                ctx.setSslKeyPair(sslKeyPair);
            } else {
                throw new IOException("Unexpected status: " + status);
            }
        } catch (Exception e) {
            logger.warn(e);
            ctx.updateError(e);
        }

    }

}
