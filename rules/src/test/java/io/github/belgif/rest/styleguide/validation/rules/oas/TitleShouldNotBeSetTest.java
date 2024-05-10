package io.github.belgif.rest.styleguide.validation.rules.oas;

import io.github.belgif.rest.styleguide.validation.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class TitleShouldNotBeSetTest extends AbstractOasRuleTest {
    @Test
    public void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testInvalidOpenApi() {
        assertErrorCount(2 , callRules("openapi_bad.yaml"));
    }
}
