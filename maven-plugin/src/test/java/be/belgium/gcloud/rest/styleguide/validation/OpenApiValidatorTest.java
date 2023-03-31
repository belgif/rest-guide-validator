package be.belgium.gcloud.rest.styleguide.validation;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * /!\ Test not working with InteliJ  /!\
 */
public class OpenApiValidatorTest {

    @Test
    void isValidTest()  {
        var file = new File(getClass().getResource("rules/swagger_bad.yaml").getFile());
        assertFalse(OpenApiValidator.isOasValid(file));

        assertFalse(OpenApiValidator.isOasValid(file, new ConsoleOutputProcessor()));

        assertFalse(OpenApiValidator.isOasValid(file, new ConsoleOutputProcessor(), new Log4JOutputProcessor()));
    }


    @Test
    void isOasValid() {
        var file = new File(getClass().getResource("rules/swagger6.yaml").getFile());
        List<String> excludedPaths = List.of(new String[]{"/api/doc/swagger.json", "/api/doc/swagger.yaml", "/api/doc",
                "/businessrules/{name}", "/businessvalues/{key}"});
        assertTrue(OpenApiValidator.isOasValid(file, excludedPaths, new ConsoleOutputProcessor()));
    }
}
