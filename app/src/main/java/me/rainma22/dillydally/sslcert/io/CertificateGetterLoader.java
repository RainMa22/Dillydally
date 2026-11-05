package me.rainma22.dillydally.sslcert.io;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.CertificateException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import me.rainma22.dillydally.conf.ConfBean;
import me.rainma22.dillydally.sslcert.certificategetter.CertificateGetterInterface;
import me.rainma22.dillydally.sslcert.certificategetter.CertificateGetterProvider;

public class CertificateGetterLoader {
    private ConfBean conf;
    private static final Logger LOGGER = LogManager.getLogger();
    static {
        if (Security.getProvider("BC") == null)
            Security.addProvider(new BouncyCastleProvider());
    }

    public CertificateGetterLoader(ConfBean conf) {
        this.conf = conf;
    }

    public CertificateGetterInterface loadCertGetter() {
        var sslConf = conf.getSslCertificateConf();

        var acmeKeyPath = Path.of(sslConf.getPathToACMEPEM());
        CertificateGetterInterface cGetter = CertificateGetterProvider.provide(conf);

        try (PEMParser parser = new PEMParser(new FileReader(acmeKeyPath.toFile()))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            var decryptorProvider = new JcePEMDecryptorProviderBuilder()
                    .setProvider(new BouncyCastleProvider())
                    .build(sslConf.getAcmePassword()
                            .toCharArray());
            PEMEncryptedKeyPair pekp = (PEMEncryptedKeyPair) parser.readObject();
            var pkp = pekp.decryptKeyPair(decryptorProvider);
            KeyPair kp = converter.getKeyPair(pkp);
            LOGGER.info("Loaded ACME keypair from file {}", acmeKeyPath);
            cGetter.setACMEKeyPair(kp);
        } catch (IOException | NullPointerException e) {
            LOGGER.warn("Could not load saved ACME PEM key, will try to regenerate.", e);
        }

        try {
            LOGGER.info("Loading SSL key and certificate");
            var sslLoader = new SSLLoader(conf);
            var certs = sslLoader.loadSSLCertificates();
            var kp = sslLoader.loadSSLKeyPair();
            // promote the state to completed if keys exists
            cGetter.setCert(kp, certs);
        } catch (CertificateException | IOException e) {
            LOGGER.warn("Problem loading Loading SSL key and cetificate", e);
        }

        return cGetter;
    }

}
