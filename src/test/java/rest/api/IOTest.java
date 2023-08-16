package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;

public class IOTest {

    @Test
    void singleLineStringFromClassPathFile()  {
        String expectedValue = "select * from users where name like 'バト%'";
        String actualValue = IO.readAsSingleLineFromClassPath("query.sql");
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void singleLineStringFromFile() {
        String expectedValue = "select * from users where name like 'バト%'";
        String actualValue = IO.readAsSingleLineFromFileSystem(Files.currentFolder().getPath() + "/src/test/resources/query.sql");
        assertEquals(expectedValue, actualValue);
    }

}
