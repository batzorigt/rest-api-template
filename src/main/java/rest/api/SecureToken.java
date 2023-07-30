package rest.api;

import org.apache.commons.lang3.StringUtils;

import kong.unirest.json.JSONObject;

public interface SecureToken {

    static String generate(JSONObject user) {
        return XSRFToken.generate(Crypto.encrypt(API.cfg.encryptionKey(), user.toString()));
    }

    static JSONObject parse(String token, long timeout) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        String[] tokens = token.split("\\.");

        if (tokens.length != 3) {
            return null;
        }

        String saltPlusToken = tokens[0] + "." + tokens[1];
        String signature = XSRFToken.sign(saltPlusToken);

        if (!signature.equals(tokens[2])) {
            return null;
        }

        try {
            if (System.currentTimeMillis() <= Long.parseLong(tokens[1]) + timeout) {
                return new JSONObject(Crypto.decrypt(API.cfg.encryptionKey(), tokens[0]));
            }

            return null;
        } catch (Throwable unused) {
            return null;
        }
    }
}
