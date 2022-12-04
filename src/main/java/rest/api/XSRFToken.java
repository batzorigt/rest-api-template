package rest.api;

import java.security.SecureRandom;

import org.apache.commons.lang3.StringUtils;

public enum XSRFToken {
    ;
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generate(String salt) {
        String saltPlusToken = salt + "." + Long.toString(System.currentTimeMillis());
        return saltPlusToken + "." + sign(saltPlusToken);
    }

    public static String generate() {
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        return generate(Base64.encode(salt));
    }

    public static boolean isValid(String token, long timeoutMiilis) {
        if (StringUtils.isBlank(token)) {
            return false;
        }

        var lastIndexOfDot = token.lastIndexOf(".");
        if (lastIndexOfDot == -1) {
            return false;
        }

        var expectedSignature = token.substring(lastIndexOfDot + 1);
        String saltPlusToken = token.substring(0, lastIndexOfDot);
        String actualSignature = sign(saltPlusToken);

        if (!actualSignature.equals(expectedSignature)) {
            return false;
        }

        lastIndexOfDot = saltPlusToken.lastIndexOf(".");
        if (lastIndexOfDot == -1) {
            return false;
        }

        try {
            var timestamp = saltPlusToken.substring(lastIndexOfDot + 1);
            return System.currentTimeMillis() <= Long.parseLong(timestamp) + timeoutMiilis;
        } catch (@SuppressWarnings("unused") NumberFormatException e) {
            return false;
        }
    }

    public static String sign(String saltPlusToken) {
        return Crypto.sign(saltPlusToken, API.config.encryptionKey());
    }

}