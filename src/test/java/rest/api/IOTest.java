package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;

public class IOTest {

    @Test
    void singleLineStringFromClassPathFile() throws Exception {
        String expectedValue = "select * from users where name like 'バト%'";
        String actualValue = IO.singleLineFromClassPath("query.sql");
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void singleLineStringFromFile() throws Exception {
        String expectedValue = "select * from users where name like 'バト%'";
        String actualValue = IO.singleLineFromFile(Files.currentFolder().getPath() + "/src/test/resources/query.sql");
        assertEquals(expectedValue, actualValue);
    }

}
