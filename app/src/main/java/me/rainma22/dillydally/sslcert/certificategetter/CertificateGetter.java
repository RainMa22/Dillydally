package me.rainma22.dillydally.sslcert.certificategetter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.openssl.PEMEncryptor;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.json.JSONException;
import org.json.JSONObject;

import me.rainma22.dillydally.conf.ConfBean;
import me.rainma22.dillydally.sslcert.io.SSLSaver;
import me.rainma22.dillydally.util.GenUtils;

/**
 *
 */
class CertificateGetter implements CertificateGetterInterface {
        private CertificateGetterContext context;
        private ConfBean conf;

        protected CertificateGetter() {
        }
        // protected CertificateGetter(ConfBean conf) throws IOException,
        // InterruptedException, NoSuchAlgorithmException {
        // this(conf, GenUtils.generateKeyPair());
        // }

        // protected CertificateGetter(ConfBean conf, KeyPair kp) throws IOException,
        // InterruptedException {
        // this.conf = conf;
        // setACMEKeyPair(kp);
        // }

        @Override
        public void setConf(ConfBean conf) {
                this.conf = conf;
        }

        @Override
        public void setACMEKeyPair(KeyPair kp) {
                context = new CertificateGetterContext(kp, conf);
        }

        @Override
        public KeyPair getACMEKeyPair() {
                return context.getAcmeKeyPair();
        }

        public Pair<KeyPair, X509Certificate[]> getCert() {
                do {
                        // check for updates if already completed
                        nextState();
                        // System.out.println(currState.getClass().getName());
                } while (context.getCertChain() == null
                                && context.getNumRetries() < conf.getSslCertificateConf().getnPollingRetries());

                return Pair.of(context.getSslKeyPair(), context.getCertChain());
        }

        public void setCert(KeyPair sslKeyPair, X509Certificate[] certs) {
                context.setCertChain(certs);
                context.setSslKeyPair(sslKeyPair);
                context.setOrderExpiry(LocalDateTime.MAX);
        }

        private void nextState() {
                context.getState().handle(context);
        }

}
