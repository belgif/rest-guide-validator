package be.belgium.gcloud.rest.styleguide.validation;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * /!\ Test not working with InteliJ  /!\
 */
public class OpenApiValidatorTest {

    @Test
    void isValidTest() throws IOException {
        var file = new File(getClass().getResource("rules/swagger_bad.yaml").getFile());
        assertFalse(OpenApiValidator.isOasValid(file));

        assertFalse(OpenApiValidator.isOasValid(file, new ConsoleOutputProcessor()));

        assertFalse(OpenApiValidator.isOasValid(file, new ConsoleOutputProcessor(), new Log4JOutputProcessor()));
    }

}
