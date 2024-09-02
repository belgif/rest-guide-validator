package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class PresenceOfPayloadTest extends AbstractOasRuleTest {
    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        assertErrorCount(9, callRules("openapi_bad.yaml"));
    }

}
