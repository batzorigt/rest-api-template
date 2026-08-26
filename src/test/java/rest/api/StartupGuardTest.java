package rest.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StartupGuardTest {

    @Test
    void localEnvironmentAcceptsDefaultKey() {
        Assertions.assertDoesNotThrow(
                () -> StartupGuard.requireSafeProductionConfig("local", Config.INSECURE_DEFAULT_KEY));
        Assertions.assertDoesNotThrow(
                () -> StartupGuard.requireSafeProductionConfig("LOCAL", Config.INSECURE_DEFAULT_KEY));
    }

    @Test
    void productionRefusesDefaultKey() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> StartupGuard.requireSafeProductionConfig("prod", Config.INSECURE_DEFAULT_KEY));
    }

    @Test
    void productionAcceptsRealKey() {
        Assertions.assertDoesNotThrow(
                () -> StartupGuard.requireSafeProductionConfig("prod", "a-real-32-byte-random-secret!!"));
    }
}
