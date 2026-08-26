package rest.api;

public interface StartupGuard {

    static void requireSafeProductionConfig(String environment, String encryptionKey) {
        if (Config.LOCAL_ENVIRONMENT.equalsIgnoreCase(environment)) {
            return;
        }

        if (Config.INSECURE_DEFAULT_KEY.equals(encryptionKey)) {
            throw new IllegalStateException(
                    "Refusing to start: encryptionKey is still the public default. Set a real key via env var or /rapit.config before running outside the local environment.");
        }
    }
}
