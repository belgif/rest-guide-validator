package be.belgium.gcloud.rest.styleguide.validation;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
public class OpenApiValidatorTest {

    @Test
    void isValidTest() throws IOException {
        var file = new File(getClass().getResource("rules/swagger_bad.yaml").getFile());
        assertFalse(OpenApiValidator.isOasValid(file));
    }
}
