package me.rainma22.dillydally.sslcert.challengecompletion;

import java.io.IOException;
import java.security.KeyPair;

import com.sun.net.httpserver.HttpServer;

import io.jsonwebtoken.security.Jwks;
import me.rainma22.dillydally.conf.ConfBean;
import me.rainma22.dillydally.conf.SSLCertificateConfBean;
import me.rainma22.dillydally.exceptions.ServerNotSetException;
import me.rainma22.dillydally.exceptions.UnsupportedCompletorTypeException;
import me.rainma22.dillydally.sslcert.OrderChallenge;

public class ChallengeCompletors {
    private static HttpServer httpServer = null;

    public static void setHttpServer(HttpServer httpServer) {
        ChallengeCompletors.httpServer = httpServer;
    }

    public static ChallengeCompletor fromConf(ConfBean conf)
            throws UnsupportedCompletorTypeException {
        SSLCertificateConfBean httpChallengeConfBean = conf.getSslCertificateConf();
        switch (httpChallengeConfBean.getType().toLowerCase()) {
            case "file":
                return fileBased(httpChallengeConfBean);
            case "handler":
                try {
                    return HandlerBased();
                } catch (ServerNotSetException e) {
                    throw new UnsupportedCompletorTypeException("httpServer not set, therefore not supported", e);
                }
            default:
                throw new UnsupportedCompletorTypeException(
                        "unsupported challenge type configured: "
                                + httpChallengeConfBean.getType());
        }
    }

    private static HandlerBasedCompletor HandlerBased() throws ServerNotSetException {
        if (httpServer == null) {
            throw new ServerNotSetException("Cannot bind a handler to an undefined server");
        }
        return new HandlerBasedCompletor(httpServer);
    }

    @Deprecated
    public static FileBasedCompletor fileBased(SSLCertificateConfBean httpChallengeConfBean) {
        return new FileBasedCompletor(httpChallengeConfBean);
    }
}

class HandlerBasedCompletor implements ChallengeCompletor {
    private HttpServer server;

    public HandlerBasedCompletor(HttpServer server) {
        this.server = server;
    }

    @Override
    public AutoCloseable completeChallenge(OrderChallenge challenge, KeyPair kp) throws IOException {
        var challengeFolderPath = String.join("/", "", ".well-known",
                "acme-challenge", challenge.getToken());
        var thumbprint = Jwks.builder().key(kp.getPublic()).build()
                .thumbprint(Jwks.HASH.SHA256).toString()
                .replaceAll("=", "");
        var content = challenge.getToken() + "." + thumbprint;
        byte[] data = content.getBytes();
        // LogManager.getLogger().error(challengeFolderPath.toString());
        var ctx = server.createContext(challengeFolderPath, (req) -> {
            req.sendResponseHeaders(200, data.length);
            req.getResponseBody().write(data);
            req.close();
        });
        return new AutoCloseable() {
            private boolean closed = false;

            @Override
            public void close() throws Exception {
                if (!closed) {
                    server.removeContext(ctx);
                    closed = true;
                }
            }
        };
    }

}
