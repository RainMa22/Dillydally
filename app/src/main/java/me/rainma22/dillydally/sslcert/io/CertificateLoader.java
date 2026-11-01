package me.rainma22.dillydally.sslcert.io;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

public class CertificateLoader {
    private static final Logger logger = LogManager.getLogger();

    public X509Certificate[] loadSSLCertificates(Path certPath) throws IOException, CertificateException {
        return loadSSLCertificatesList(certPath).stream().toArray(X509Certificate[]::new);
    }

    public List<X509Certificate> loadSSLCertificatesList(Path certPath) throws IOException, CertificateException {
        logger.info("Loading ssl certificate from {}", certPath);
        try (PEMParser parser = new PEMParser(new FileReader(certPath.toFile()))) {
            JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
            Object obj;
            List<X509Certificate> certList = new ArrayList<>();
            while ((obj = parser.readObject()) != null) {
                certList.add(converter.getCertificate((X509CertificateHolder) obj));
            }
            logger.info("Loaded {} ssl certificate{} from {}", certList.size(),
                    certList.size() == 1 ? "" : "s",
                    certPath);
            return certList;
        }
    }
}
