package me.rainma22.dillydally.sslcert;

import java.util.List;

public class AuthChallengeResponse {
    private String status;
    private String expires;
    private OrderIdentifierBean identifier;
    private List<OrderChallengeBean> challenges;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public OrderIdentifierBean getIdentifier() {
        return identifier;
    }

    public void setIdentifier(OrderIdentifierBean identifier) {
        this.identifier = identifier;
    }

    public List<OrderChallengeBean> getChallenges() {
        return challenges;
    }

    public void setChallenges(List<OrderChallengeBean> challenges) {
        this.challenges = challenges;
    }

}
