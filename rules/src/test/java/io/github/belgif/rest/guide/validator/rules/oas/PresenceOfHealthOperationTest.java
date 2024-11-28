package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class PresenceOfHealthOperationTest extends AbstractOasRuleTest {
    @Test
    void testValidOpenapi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testMissingHealthPath() {
        assertErrorCount(1, callRules("missingHealthPath.yaml"));
    }

    @Test
    void testOpenApiWithoutPaths() {
        assertNoViolations(callRules("noPaths.yaml"));
    }

    @Test
    void testHealthOperationWithoutProperResponse() {
        assertErrorCount(1, callRules("healthOperationWithoutProperResponse.yaml"));
    }

    @Test
    void testHealthOperationWithoutGET() {
        assertErrorCount(1, callRules("healthOperationWithoutGet.yaml"));
    }
}
