package me.rainma22.dillydally.sslcert.io;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import me.rainma22.dillydally.conf.ConfBean;

public class SSLLoader {
    private ConfBean conf;
    private static final Logger logger = LogManager.getLogger();

    public SSLLoader(ConfBean conf) {
        this.conf = conf;
    }

    public X509Certificate[] loadSSLCertificates() throws IOException, CertificateException {
        var sslConf = conf.getSslCertificateConf();
        return new CertificateLoader().loadSSLCertificates(Path.of(sslConf.getPathToSSLCertPEM()));
    }

    public KeyPair loadSSLKeyPair() throws IOException {
        var sslConf = conf.getSslCertificateConf();
        KeyPair kp;
        logger.info("Loading ssl keypair from {}", sslConf.getPathToSSLKeyPEM());
        try (PEMParser parser = new PEMParser(new FileReader(sslConf.getPathToSSLKeyPEM()))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            var decryptorProvider = new JcePEMDecryptorProviderBuilder()
                    .setProvider(new BouncyCastleProvider())
                    .build(sslConf.getSslKeyPassword().toCharArray());
            PEMEncryptedKeyPair pekp = (PEMEncryptedKeyPair) parser.readObject();
            var pkp = pekp.decryptKeyPair(decryptorProvider);
            kp = converter.getKeyPair(pkp);
        }
        logger.info("Loaded ssl keypair from {}", sslConf.getPathToSSLKeyPEM());
        return kp;
    }

}
