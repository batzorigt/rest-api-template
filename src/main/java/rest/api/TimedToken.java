package rest.api;

import org.apache.commons.lang3.StringUtils;

final class TimedToken {

    private TimedToken() {
    }

    static String generate(String payload) {
        String timestampedPayload = payload + "." + System.currentTimeMillis();
        return timestampedPayload + "." + Crypto.sign(timestampedPayload, Server.cfg.encryptionKey());
    }

    static String payloadIfValid(String token, long timeoutMillis) {
        if (StringUtils.isBlank(token) || timeoutMillis < 0) {
            return null;
        }

        int signatureSeparator = token.lastIndexOf('.');
        int timestampSeparator = signatureSeparator < 0 ? -1 : token.lastIndexOf('.', signatureSeparator - 1);
        if (timestampSeparator < 0) {
            return null;
        }

        String timestampedPayload = token.substring(0, signatureSeparator);
        String receivedSignature = token.substring(signatureSeparator + 1);
        String expectedSignature = Crypto.sign(timestampedPayload, Server.cfg.encryptionKey());
        if (!Crypto.constantTimeEquals(expectedSignature, receivedSignature)) {
            return null;
        }

        try {
            long issuedAt = Long.parseLong(token.substring(timestampSeparator + 1, signatureSeparator));
            long now = System.currentTimeMillis();
            long oldestAllowed = timeoutMillis >= now ? 0 : now - timeoutMillis;
            if (issuedAt < oldestAllowed || issuedAt > now) {
                return null;
            }
            return token.substring(0, timestampSeparator);
        } catch (NumberFormatException unused) {
            return null;
        }
    }
}
