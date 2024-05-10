package io.github.belgif.rest.styleguide.validation.rules.oas;

import io.github.belgif.rest.styleguide.validation.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class ShouldUseProblemResponseSchemaTest extends AbstractOasRuleTest {
    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        assertErrorCount(3, callRules("swagger_bad.yaml"));
    }

    @Test
    public void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testInvalidOpenApi() {
        assertErrorCount(3, callRules("openapi_bad.yaml"));
    }

}
