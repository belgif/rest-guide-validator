package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

@Getter
class ScopesAreDefinedInSecuritySchemeTest extends AbstractOasRuleTest {

    @Test
    void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testInvalidOpenApi() {
        assertErrorCount(1, callRules("invalidOpenapi.yaml"));
    }

    @Test
    void testInvalidGlobalOpenApi() {
        assertErrorCount(1, callRules("invalidGlobalOpenapi.yaml"));
    }
}
