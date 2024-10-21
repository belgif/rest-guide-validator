package io.github.belgif.rest.guide.validator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiValidatorTest {

    @Test
    void isOasValid() {
        var file = new File(Objects.requireNonNull(getClass().getResource("rules/openapi.yaml")).getFile());
        assertTrue(OpenApiValidator.callRules(file, null).isOasValid());
    }
}
