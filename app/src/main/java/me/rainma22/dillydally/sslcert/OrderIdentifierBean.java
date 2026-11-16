package me.rainma22.dillydally.sslcert;

import me.rainma22.dillydally.abstracts.Bean;

public class OrderIdentifierBean extends Bean {
    String type;
    String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
