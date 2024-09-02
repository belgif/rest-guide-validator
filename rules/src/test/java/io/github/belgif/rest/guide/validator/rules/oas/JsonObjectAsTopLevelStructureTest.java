package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class JsonObjectAsTopLevelStructureTest extends AbstractOasRuleTest {

    @Test
    public void testValid() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

    @Test
    public void testInvalidOpenApi() {
        assertErrorCount(6, callRules("openapi_bad.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        assertErrorCount(5, callRules("swagger_bad.yaml"));
    }

}
