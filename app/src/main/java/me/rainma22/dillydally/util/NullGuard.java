package me.rainma22.dillydally.util;

public class NullGuard {
    public static void mustBeNotNull(Object o) {
        mustBeNotNull(o, "Object");
    }

    public static void mustBeNotNull(Object o, String name) throws NullPointerException {
        if (o == null)
            throw new NullPointerException("%s must not be null.".formatted(name));
    }
}
