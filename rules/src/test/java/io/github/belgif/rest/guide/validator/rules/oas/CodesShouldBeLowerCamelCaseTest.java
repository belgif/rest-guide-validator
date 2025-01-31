package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

 class CodesShouldBeLowerCamelCaseTest extends AbstractOasRuleTest {
    @Test
     void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
     void testInvalidOpenApi() {
        assertErrorCount(2, callRules("openapi_bad.yaml"));
    }

    @Test
     void testInvalidOpenApiWithSortingEnums() {
        assertErrorCount(2, callRules("openapi_bad_sorting.yaml"));
    }

    @Test
     void invalidEnumShouldBeIgnored() {
        assertErrorCount(1, callRules("invalidEnum.yaml"));
    }
}
