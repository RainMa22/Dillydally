package me.rainma22.dillydally.builder;

import java.io.IOException;
import java.net.http.HttpClient;
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
    public static HttpClient.Builder newBuilder(Optional<ConfBean> confMaybe) {
        List<X509Certificate> extraCaCerts = new ArrayList<>(2);

        if (confMaybe.isPresent()) {
            CertificateLoader certLoader = new CertificateLoader();
            confMaybe.get().getExtraCaCerts()
                    .stream()
                    .map(Path::of)
                    .forEach(path -> {
                        try {
                            extraCaCerts.add(certLoader.loadSSLCertificatesList(path).getLast());
                        } catch (CertificateException | IOException | ArrayIndexOutOfBoundsException e) {
                            logger.warn("Error Loading Certificate from path {}({}), Skipping.",path, e);
                        }
                    });
        }
        return HttpClient.newBuilder()
                .sslContext(TrustUtils.WithAdditionalCertificate(extraCaCerts.stream().toArray(X509Certificate[]::new)));
    }

}
