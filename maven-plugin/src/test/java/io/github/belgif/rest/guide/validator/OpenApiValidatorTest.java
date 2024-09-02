package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.output.ConsoleOutputProcessor;
import io.github.belgif.rest.guide.validator.output.OutputGroupBy;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class OpenApiValidatorTest {

    @Test
    void isOasValid() {
        var file = new File(getClass().getResource("rules/openapi.yaml").getFile());
        assertTrue(OpenApiValidator.isOasValid(file, null, new ConsoleOutputProcessor(OutputGroupBy.RULE)));
    }
}
