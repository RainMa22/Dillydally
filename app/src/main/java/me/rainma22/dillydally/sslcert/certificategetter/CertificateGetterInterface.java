package me.rainma22.dillydally.sslcert.certificategetter;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.apache.commons.lang3.tuple.Pair;

public interface CertificateGetterInterface {

    Pair<KeyPair, X509Certificate[]> getCert();

    void setCert(KeyPair sslKeyPair, X509Certificate[] certs);

    void setACMEKeyPair(KeyPair kp);
    KeyPair getACMEKeyPair();
}
