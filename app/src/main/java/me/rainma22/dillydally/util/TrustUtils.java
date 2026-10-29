package me.rainma22.dillydally.util;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class TrustUtils {
    public static SSLContext TRUST_ALL_CONTEXT = null;
    static {
        TrustManager[] trustAll = {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                }
        };
        try {
            TRUST_ALL_CONTEXT = SSLContext.getInstance("TLS");
            TRUST_ALL_CONTEXT.init(null, trustAll, new java.security.SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
