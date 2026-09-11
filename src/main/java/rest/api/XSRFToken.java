package rest.api;

import java.security.SecureRandom;

public enum XSRFToken {
    ;
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generate(String salt) {
        return TimedToken.generate(salt);
    }

    public static String generate() {
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        return generate(Base64.encode(salt));
    }

    public static boolean isValid(String token, long timeoutMillis) {
        return TimedToken.payloadIfValid(token, timeoutMillis) != null;
    }

    public static String sign(String saltPlusToken) {
        return Crypto.sign(saltPlusToken, Server.cfg.encryptionKey());
    }

}
