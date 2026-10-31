package me.rainma22.dillydally.sslcert.certificategetter.states;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

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

import me.rainma22.dillydally.sslcert.certificategetter.CertificateGetterContext;

public class SelfSignState implements CertificateGetterState {
        private static Logger logger = LogManager.getLogger();

        @Override
        public void handle(CertificateGetterContext ctx) {
                try {
                        var gen = KeyPairGenerator.getInstance("RSA");
                        gen.initialize(2048);
                        var sslKeyPair = gen.genKeyPair();
                        ctx.setSslKeyPair(sslKeyPair);
                        var domains = ctx.getConf().getDomains();
                        var name = new X500Name(String.format("CN=%s", ctx.getConf().getDomains().getFirst()));
                        var certBuilder = new JcaX509v3CertificateBuilder(name,
                                        BigInteger.valueOf(System.currentTimeMillis()),
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
                                                                        .skip(1)
                                                                        .map(str -> new GeneralName(GeneralName.dNSName,
                                                                                        str))
                                                                        .toArray(GeneralName[]::new)));
                        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                                        .setProvider("BC")
                                        .build(sslKeyPair.getPrivate());
                        ctx.setCertChain(new X509Certificate[] {
                                        new JcaX509CertificateConverter().getCertificate(
                                                        certBuilder.build(signer))
                        });
                } catch (CertIOException | NoSuchAlgorithmException | OperatorCreationException
                                | CertificateException e) {
                        logger.warn(e);
                        ctx.updateError(e);
                }
        }

}
