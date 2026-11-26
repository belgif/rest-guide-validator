package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class PropertiesCamelCaseTest extends AbstractOasRuleTest {
    @Test
    void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testInvalidOpenApi() {
        assertErrorCount(2, callRules("openapi_bad.yaml"));
    }

    @Test
    void testReferenceToAsynchronousMessage() {
        assertNoViolations(callRules("referencingAsynchronousMessage.yaml"));
    }

    @Test
    void testReferenceToAsynchronousMessageAndIllegalNonLowerCamelCaseProperties() {
        assertErrorCount(10, callRules("referencingAsynchronousMessageIllegalPropertyNames.yaml"));
    }
}
