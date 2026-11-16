package me.rainma22.dillydally.sslcert.challengecompletion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

import io.jsonwebtoken.security.Jwks;
import me.rainma22.dillydally.conf.SSLCertificateConfBean;
import me.rainma22.dillydally.sslcert.OrderChallengeBean;

@Deprecated
class FileBasedCompletor implements ChallengeCompletor {
    private SSLCertificateConfBean httpConf;

    @Deprecated
    public FileBasedCompletor(SSLCertificateConfBean httpChallengeConfBean) throws UnsupportedOperationException {
        httpConf = httpChallengeConfBean;
        if (!httpChallengeConfBean.getType().equalsIgnoreCase("file")) {
            throw new UnsupportedOperationException(
                    "unsupported challenge type configured: "
                            + httpChallengeConfBean.getType());
        }
    }

    @Deprecated
    public AutoCloseable completeChallenge(OrderChallengeBean challenge, KeyPair kp) throws IOException {
        var challengeFolderPath = Path.of(httpConf.getPathToWebRootDir(), ".well-known",
                "acme-challenge");
        Files.createDirectories(challengeFolderPath);
        var challengeFilePath = challengeFolderPath.resolve(challenge.getToken());
        var thumbprint = Jwks.builder().key(kp.getPublic()).build()
                .thumbprint(Jwks.HASH.SHA256).toString()
                .replaceAll("=", "");
        Files.createFile(challengeFilePath);
        Files.writeString(challengeFilePath, challenge.getToken() + "." + thumbprint);
        return () -> {
            Files.deleteIfExists(challengeFilePath);
        };
    }
}