package be.belgium.gcloud.rest.styleguide.validation.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class OpenApiViolationAggregatorTest {

    OpenApiViolationAggregator getOAS() throws IOException {
        var openApiViolationAggregator = new OpenApiViolationAggregator();
        var file = new File(OpenApiViolationAggregatorTest.class.getResource("../rules/swagger_bad.yaml").getFile());
        ApiFunctions.buildOpenApiSpecification(file, openApiViolationAggregator);
        return openApiViolationAggregator;
    }

    @Test
    void getLineNumber() throws IOException {
        var line = getOAS().getLineNumber("swagger:");
        assertTrue(line > 0);
    }

    @Test
    void testGetLineNumber() throws IOException {
        var openApiViolationAggregator = getOAS();
        var pathLine = openApiViolationAggregator.getLineNumber("/userInfo/");
        var getLine = openApiViolationAggregator.getLineNumber(pathLine, "get");
        assertTrue(pathLine > 0);
        assertTrue(getLine > pathLine);
    }

}