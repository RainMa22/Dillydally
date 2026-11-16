package me.rainma22.dillydally.sslcert.challengecompletion;

import java.io.IOException;
import java.security.KeyPair;

import me.rainma22.dillydally.sslcert.OrderChallengeBean;

public interface ChallengeCompletor {

    AutoCloseable completeChallenge(OrderChallengeBean challenge, KeyPair kp) throws IOException;

}
