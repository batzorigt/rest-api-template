package rest.api;

import org.json.JSONObject;

public interface SecureToken {

    static String generate(JSONObject user) {
        return XSRFToken.generate(Crypto.encrypt(Server.cfg.encryptionKey(), user.toString()));
    }

    static JSONObject parse(String token, long timeout) {
        String encryptedPayload = TimedToken.payloadIfValid(token, timeout);
        if (encryptedPayload == null) {
            return null;
        }

        try {
            return new JSONObject(Crypto.decrypt(Server.cfg.encryptionKey(), encryptedPayload));
        } catch (Exception unused) {
            return null;
        }
    }
}
