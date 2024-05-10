package io.github.belgif.rest.styleguide.validation;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * /!\ Test not working with InteliJ  /!\
 */
public class OpenApiValidatorTest {

    @Test
    void isOasValid() {
        var file = new File(getClass().getResource("rules/swagger6.yaml").getFile());
        List<String> excludedPaths = new ArrayList<>();
        assertTrue(OpenApiValidator.isOasValid(file, excludedPaths, null, new ConsoleOutputProcessor()));
    }
}
