package me.rainma22.dillydally.sslcert.certificategetter.states;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;

import me.rainma22.dillydally.sslcert.ResourceLocationResponse;
import me.rainma22.dillydally.sslcert.certificategetter.CertificateGetterContext;
// import me.rainma22.dillydally.util.TestUtils;

public class GetResourceLocationState implements CertificateGetterState {

    @Override
    public void handle(CertificateGetterContext ctx) {
        var conf = ctx.getConf();
        try (HttpClient client = HttpClient.newBuilder()
                // .sslContext(TestUtils.TRUST_ALL_CONTEXT)
                .build()) {
            var req = HttpRequest.newBuilder(URI.create(conf.getServerUrl()))
                    .GET()
                    .build();
            try (var in = client.send(req, BodyHandlers.ofInputStream()).body()) {
                String resLocStr = new String(in.readAllBytes());
                ctx.setResourceLocations(JSONObject.fromJson(
                        resLocStr,
                        ResourceLocationResponse.class));
            } catch (Exception e) {
                LogManager.getLogger().warn(e);
                ctx.updateError(e);
            }
        }
    }

}
