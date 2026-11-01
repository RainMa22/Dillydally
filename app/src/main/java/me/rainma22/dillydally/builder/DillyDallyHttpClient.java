package me.rainma22.dillydally.builder;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.rainma22.dillydally.conf.ConfBean;
import me.rainma22.dillydally.sslcert.io.CertificateLoader;
import me.rainma22.dillydally.util.TrustUtils;

public class DillyDallyHttpClient {
    private static final Logger logger = LogManager.getLogger();

    public static HttpClient.Builder newBuilder() {
        return newBuilder(Optional.empty());
    }

    public static HttpClient.Builder newBuilder(Optional<ConfBean> confMaybe) {
        List<X509Certificate> extraCaCerts = new ArrayList<>(2);

        if (confMaybe.isPresent()) {
            CertificateLoader certLoader = new CertificateLoader();
            confMaybe.get().getExtraCaCerts()
                    .stream()
                    .map((str) -> {
                        try {
                            return Path.of(str);
                        } catch (InvalidPathException e) {
                            logger.warn("Invalid path for ca certificate %s, Skipping.".formatted(str), e);
                            return null;
                        }
                    })
                    .filter(p -> p != null)
                    .forEach(path -> {
                        try {
                            var caCerts = certLoader.loadSSLCertificatesList(path)
                                    .stream()
                                    .filter(c -> c.getBasicConstraints() >= 0) // CA Cert check
                                    .toList();
                            if (caCerts.isEmpty()) {
                                logger.warn("No valid CA certificates found for file {}, skipping", path);
                            }
                            extraCaCerts.addAll(caCerts);
                        } catch (CertificateException | IOException | ClassCastException e) {
                            logger.warn("Error Loading Certificate from path %s, Skipping.".formatted(path), e);
                        }
                    });
        }
        return HttpClient.newBuilder()
                .sslContext(
                        TrustUtils.WithAdditionalCertificate(extraCaCerts.stream().toArray(X509Certificate[]::new)));
    }

}
