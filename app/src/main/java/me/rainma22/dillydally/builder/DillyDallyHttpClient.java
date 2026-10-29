package me.rainma22.dillydally.builder;

import java.net.http.HttpClient;
import java.security.cert.X509Certificate;

import me.rainma22.dillydally.util.TrustUtils;

public class DillyDallyHttpClient {
    public static HttpClient.Builder newBuilder(){
        // TODO: custom certificate configuration for easier testing with test ACME servers.
        return HttpClient.newBuilder()
            .sslContext(TrustUtils.WithAdditionalCertificate(new X509Certificate[0]));
    }
    
}
