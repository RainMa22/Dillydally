package me.rainma22.dillydally.util;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import me.rainma22.dillydally.exceptions.DillyDallyException;
import me.rainma22.dillydally.exceptions.DillyDallyRuntimeException;

public class TrustUtils {
    public static SSLContext TRUST_ALL_CONTEXT = null;
    static {
        TrustManager[] trustAll = {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(
                            X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(
                            X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        try {
            TRUST_ALL_CONTEXT = SSLContext.getInstance("TLS");
            TRUST_ALL_CONTEXT.init(null, trustAll, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SSLContext WithAdditionalCertificate(X509Certificate[] certs) {
        try {
            var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            X509TrustManager trustManager = Stream.of(tmf.getTrustManagers())
                    .filter(x -> x instanceof X509TrustManager)
                    .map(x -> (X509TrustManager) x)
                    .findFirst()
                    .orElseThrow(() -> new DillyDallyException("No Default X509TrustManager found"));

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            int i = 0;
            for(var cert : trustManager.getAcceptedIssuers()){
                ks.setCertificateEntry("ca-%d".formatted(i++), cert);
            }
            for(var cert : certs){
                ks.setCertificateEntry("ca-%d".formatted(i++), cert);
            }
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new DillyDallyRuntimeException(e);
        }
    }

}
