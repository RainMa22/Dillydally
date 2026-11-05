package me.rainma22.dillydally.sslcert.certificategetter;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import me.rainma22.dillydally.conf.ConfBean;

/**
 * SelfSignCertificateGetter
 */
public class SelfSignCertificateGetter implements CertificateGetterInterface {
    private static Logger logger = LogManager.getLogger();
    private KeyPair acmeKeyPair = null;

    private KeyPair sslKeyPair = null;
    private X509Certificate[] cert = null;
    private final ConfBean conf;

    public SelfSignCertificateGetter(ConfBean conf) {
        this.conf = conf;
    }

    @Override
    public KeyPair getACMEKeyPair() {
        return acmeKeyPair;
    }

    @Override
    public void setACMEKeyPair(KeyPair kp) {
        acmeKeyPair = kp;
    }

    @Override
    public Pair<KeyPair, X509Certificate[]> getCert() {
        if (sslKeyPair != null && cert != null) {
            return Pair.of(sslKeyPair, cert);
        }
        try {
            var gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            sslKeyPair = sslKeyPair != null ? sslKeyPair : gen.genKeyPair();
            var domains = conf.getDomains();
            var name = new X500Name(String.format("CN=%s", conf.getDomains().getFirst()));
            var certBuilder = new JcaX509v3CertificateBuilder(name,
                    BigInteger.valueOf(System.currentTimeMillis())
                            .add(BigInteger.valueOf(new SecureRandom().nextLong())),
                    Date.from(Instant.now()),
                    Date.from(Instant.now().plus(Duration.ofDays(3650))), name,
                    sslKeyPair.getPublic())
                    .addExtension(
                            Extension.basicConstraints,
                            true, // Critical
                            new BasicConstraints(false) // Not a CA
                    )
                    .addExtension(
                            Extension.keyUsage,
                            true,
                            new KeyUsage(KeyUsage.digitalSignature
                                    | KeyUsage.keyEncipherment))
                    .addExtension(
                            Extension.subjectAlternativeName,
                            false,
                            new GeneralNames(domains.stream()
                                    .map(str -> new GeneralName(GeneralName.dNSName,
                                            str))
                                    .toArray(GeneralName[]::new)));
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider("BC")
                    .build(sslKeyPair.getPrivate());
            cert = new X509Certificate[] {
                    new JcaX509CertificateConverter().getCertificate(
                            certBuilder.build(signer)) };
            return Pair.of(sslKeyPair, cert);
        } catch (CertIOException | NoSuchAlgorithmException | OperatorCreationException
                | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setCert(KeyPair sslKeyPair, X509Certificate[] certs) {
        this.sslKeyPair = sslKeyPair;
        this.cert = certs;
    }

}
