package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

 class DefaultProblemResponseTest extends AbstractOasRuleTest {
    @Test
     void testValid() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
     void testInvalidOpenApi() {
        assertErrorCount(1, callRules("openapi_bad.yaml"));
    }

}
