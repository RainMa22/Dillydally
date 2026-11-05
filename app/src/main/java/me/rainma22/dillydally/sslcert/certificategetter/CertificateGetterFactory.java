package me.rainma22.dillydally.sslcert.certificategetter;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.function.Function;

import me.rainma22.dillydally.conf.ConfBean;

public class CertificateGetterFactory {
    private static final HashMap<String, Function<ConfBean, CertificateGetterInterface>> providers = new HashMap<>(4);
    private static final Function<ConfBean, CertificateGetterInterface> DEFAULT_CERTIFICATE_GETTER_CONSTUCTOR = conf -> {
        try {
            return new CertificateGetter(conf);
        } catch (NoSuchAlgorithmException | IOException | InterruptedException e) {
            throw new Error(e);
        }
    };
    static {
        providers.put(ConfBean.SELF_SIGN, conf -> new SelfSignCertificateGetter(conf));
    }

    public static CertificateGetterInterface getDefault(ConfBean conf) {
        return DEFAULT_CERTIFICATE_GETTER_CONSTUCTOR.apply(conf);
    }

    public static CertificateGetterInterface provide(ConfBean conf) {
        var name = conf.getServerUrl();
        return providers.getOrDefault(name, DEFAULT_CERTIFICATE_GETTER_CONSTUCTOR).apply(conf);
    }
}
