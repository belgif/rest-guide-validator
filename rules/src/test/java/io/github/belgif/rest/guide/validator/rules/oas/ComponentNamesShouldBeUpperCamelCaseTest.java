package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class ComponentNamesShouldBeUpperCamelCaseTest extends AbstractOasRuleTest {
    @Test
    public void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }
    @Test
    public void testLowerCamelCase() {
        assertErrorCount(1, callRules("lowerCamelCase.yaml"));
    }
    @Test
    public void testAllComponentTypesDetected() {
        assertErrorCount(8, callRules("allComponentTypesDetected.yaml"));
    }
    @Test
    public void testStartsWithNumber() {
        assertErrorCount(1, callRules("startsWithNumber.yaml"));
    }
    @Test
    public void testUpperCase() {
        assertErrorCount(1, callRules("upperCase.yaml"));
    }
    @Test
    public void testOtherSeparators() {
        assertErrorCount(3, callRules("otherSeparators.yaml"));
    }
    @Test
    public void testOas2() {
        assertErrorCount(3, callRules("invalidSwagger.yaml"));
    }
}
