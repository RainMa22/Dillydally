package me.rainma22.dillydally.sslcert.certificategetter;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.function.Function;

import me.rainma22.dillydally.conf.ConfBean;
import me.rainma22.dillydally.util.GenUtils;

public class CertificateGetterFactory {
    /**
     * CertificateGetterType
     */
    public enum CertificateGetterType {
        SELF_SIGN,
        ACME
    }

    private CertificateGetterType type = CertificateGetterType.ACME;
    private ConfBean conf = null;
    private KeyPair acmeKeyPair = null;
    private KeyPair sslKeyPair = null;
    private X509Certificate[] certs = null;

    private CertificateGetterFactory ofType(String type) {
        this.type = switch (type.toLowerCase()) {
            case ConfBean.SELF_SIGN -> CertificateGetterType.SELF_SIGN;
            default -> CertificateGetterType.ACME;
        };
        return this;
    }

    /**
     * Creates a CertificateGetterFactory
     */
    public CertificateGetterFactory() {
    }

    public CertificateGetterFactory ofConf(ConfBean conf) {
        this.conf = conf;
        return this.ofType(conf.getServerUrl());
    }

    public CertificateGetterFactory withACMEKeyPair(KeyPair acmeKeyPair) {
        this.acmeKeyPair = acmeKeyPair;
        return this;
    }

    public CertificateGetterFactory withCertificate(KeyPair sslKeyPair, X509Certificate[] certs) {
        this.sslKeyPair = sslKeyPair;
        this.certs = certs;
        return this;
    }

    public CertificateGetterInterface get() {
        if (type == null)
            throw new IllegalStateException("no Type specified for CertificateGetterFactory");
        if (conf == null)
            throw new IllegalStateException("no ConfBean specified for CertificateGetterFactory");
        if (acmeKeyPair == null && type.equals(CertificateGetterType.ACME))
            try {
                acmeKeyPair = GenUtils.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Error when generating an acme keypair to create an acme CertificateGetter");
            }
        var res = (type.equals(CertificateGetterType.SELF_SIGN)) ? new SelfSignCertificateGetter()
                : new CertificateGetter();
        res.setConf(conf);
        res.setACMEKeyPair(acmeKeyPair);
        res.setCert(sslKeyPair, certs);
        return res;
    }
}
