package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.aeonbits.owner.ConfigCache;
import org.junit.jupiter.api.Test;

public class AppConfigTest {

    @Test
    public void testAll() {
        // Singleton
        AppConfig cfg = ConfigCache.getOrCreate(AppConfig.class);

        // read value from environment variable
        assertNotNull(cfg.USER());
        // default value will be returned
        assertEquals("name", cfg.name());
        // parameterized configuration
        assertEquals("Bat is 16.", cfg.age("Bat", 16));
        // variable expansion
        assertEquals("1 is less than 2.", cfg.oneIsLessThanTwo());
        // accessible: get any value without strict interface definition
        assertEquals("test", cfg.getProperty("environment"));
        // read value from configuration file which is located in class path
        assertEquals("This value is retrieved from configuration file.", cfg.readFromClassPathConfigFile());
    }
}
