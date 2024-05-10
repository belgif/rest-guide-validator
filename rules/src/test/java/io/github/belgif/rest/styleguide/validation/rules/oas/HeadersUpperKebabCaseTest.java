package io.github.belgif.rest.styleguide.validation.rules.oas;

import io.github.belgif.rest.styleguide.validation.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class HeadersUpperKebabCaseTest extends AbstractOasRuleTest {
    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        assertErrorCount(4, callRules("openapi_bad.yaml"));
    }

}
