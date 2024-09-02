package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

@Getter
public class ReadOnlyPropertiesTest extends AbstractOasRuleTest {
    @Test
    public void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testValidOpenApiEdgeCase() {
        assertNoViolations(callRules("openapi_edge_case.yaml"));
    }

    @Test
    public void testInvalidOpenApi() {
        assertErrorCount(3, callRules("openapi_bad.yaml"));
    }
}
