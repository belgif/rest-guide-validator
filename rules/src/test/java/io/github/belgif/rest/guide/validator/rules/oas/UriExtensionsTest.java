package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

 class UriExtensionsTest extends AbstractOasRuleTest {

    @Test
     void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
     void testInvalidOpenApi() {
        assertViolations(callRules("openapi_bad.yaml"));
    }
}
