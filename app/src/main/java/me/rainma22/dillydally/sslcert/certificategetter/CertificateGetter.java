package me.rainma22.dillydally.sslcert.certificategetter;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import org.apache.commons.lang3.tuple.Pair;
import me.rainma22.dillydally.conf.ConfBean;

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
