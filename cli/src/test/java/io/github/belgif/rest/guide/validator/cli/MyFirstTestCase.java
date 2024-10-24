package io.github.belgif.rest.guide.validator.cli;

import io.github.belgif.rest.guide.validator.OpenApiValidator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MyFirstTestCase {

    @Test
    void test() {
        List<String> excludedFiles = new ArrayList<>();
        File myFile = new File("C:/dev/prj/belgif/rest-guide-validator/integrationtest/src/it/externalReferencesFail/openapi.yaml");
        var report = OpenApiValidator.callRules(myFile, excludedFiles);
        assertNotNull(report);
    }

}
