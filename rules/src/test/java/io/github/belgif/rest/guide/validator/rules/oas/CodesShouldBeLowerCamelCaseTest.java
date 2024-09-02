package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class CodesShouldBeLowerCamelCaseTest extends AbstractOasRuleTest {
    @Test
    public void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testInvalidOpenApi() {
        assertErrorCount(2, callRules("openapi_bad.yaml"));
    }

    @Test
    public void testInvalidOpenApiWithSortingEnums() {
        assertErrorCount(2, callRules("openapi_bad_sorting.yaml"));
    }

    @Test
    public void invalidEnumShouldBeIgnored() {
        assertErrorCount(1, callRules("invalidEnum.yaml"));
    }
}
