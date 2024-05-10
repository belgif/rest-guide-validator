package io.github.belgif.rest.guide.validator.rules.oas3;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class ApplicationJsonShouldHaveSchemaTest extends AbstractOasRuleTest {
    @Test
    public void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testInvalidOpenApi() {
        assertErrorCount(4, callRules("openapi_bad.yaml"));
    }

    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

    //Should be valid, this rule does not apply on oas2 files.
    @Test
    public void testInvalidSwagger() {
        assertErrorCount(0, callRules("swagger_bad.yaml"));
    }
}
