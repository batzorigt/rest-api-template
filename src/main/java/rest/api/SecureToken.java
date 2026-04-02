package rest.api;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public interface SecureToken {

    static String generate(JSONObject user) {
        return XSRFToken.generate(Crypto.encrypt(API.cfg.encryptionKey(), user.toString()));
    }

    static JSONObject parse(String token, long timeout) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        int lastIndexOfDot = token.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return null;
        }

        int secondLastIndexOfDot = token.lastIndexOf('.', lastIndexOfDot - 1);
        if (secondLastIndexOfDot == -1) {
            return null;
        }

        String encryptedPayload = token.substring(0, secondLastIndexOfDot);
        String timestamp = token.substring(secondLastIndexOfDot + 1, lastIndexOfDot);
        String receivedSignature = token.substring(lastIndexOfDot + 1);
        String saltPlusToken = encryptedPayload + "." + timestamp;
        String expectedSignature = XSRFToken.sign(saltPlusToken);

        if (!Crypto.constantTimeEquals(expectedSignature, receivedSignature)) {
            return null;
        }

        try {
            if (System.currentTimeMillis() <= Long.parseLong(timestamp) + timeout) {
                return new JSONObject(Crypto.decrypt(API.cfg.encryptionKey(), encryptedPayload));
            }

            return null;
        } catch (Exception unused) {
            return null;
        }
    }
}
