package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

 class ErrorResponseShouldProduceProblemJsonTest extends AbstractOasRuleTest {
    @Test
     void testValid() {
        assertNoViolations(callRules("openapi.yaml"));
    }
    @Test
     void testInvalid() {
        assertErrorCount(5, callRules("openapi_bad.yaml"));
    }
    @Test
     void testHealthCheck() {
        assertNoViolations(callRules("openapi_health_exception.yaml"));
    }
}
