package io.github.belgif.rest.guide.validator.rules;

import io.github.belgif.rest.guide.validator.OpenApiValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

class OpenApiValidatorTest {

    @Test
    void isOasValid() {
        var file = new File(Objects.requireNonNull(getClass().getResource("openapi.yaml")).getFile());
        Assertions.assertTrue(new OpenApiValidator().callRules(file, null).isOasValid());
    }
}
