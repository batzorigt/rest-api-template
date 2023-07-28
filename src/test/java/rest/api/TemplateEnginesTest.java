package rest.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import gg.jte.output.StringOutput;

public class TemplateEnginesTest {

    @Test
    void test() {
    	var output = new StringOutput();
        var templateEngine = TemplateEngines.jteEngine();
        templateEngine.render("hello.jte", "World", output);
        
        Assertions.assertTrue(output.toString().contains("Hello World!"));
        Assertions.assertTrue(output.toString().contains("The current timestamp is "));
        System.out.println(output);
    }
}
