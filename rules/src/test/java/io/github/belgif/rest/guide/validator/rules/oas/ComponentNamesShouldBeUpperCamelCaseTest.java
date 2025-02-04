package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

 class ComponentNamesShouldBeUpperCamelCaseTest extends AbstractOasRuleTest {
    @Test
     void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
     void testLowerCamelCase() {
        assertErrorCount(1, callRules("lowerCamelCase.yaml"));
    }

    @Test
     void testAllComponentTypesDetected() {
        assertErrorCount(8, callRules("allComponentTypesDetected.yaml"));
    }

    @Test
     void testStartsWithNumber() {
        assertErrorCount(1, callRules("startsWithNumber.yaml"));
    }

    @Test
     void testUpperCase() {
        assertErrorCount(1, callRules("upperCase.yaml"));
    }

    @Test
     void testOtherSeparators() {
        assertErrorCount(3, callRules("otherSeparators.yaml"));
    }
}
