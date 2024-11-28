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
    void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

    @Test
    void testInvalidOpenApi() {
        assertErrorCount(1, callRules("invalidOpenapi.yaml"));
    }

    @Test
    void testInvalidGlobalOpenApi() {
        assertErrorCount(1, callRules("invalidGlobalOpenapi.yaml"));
    }

    @Test
    void testInvalidSwagger() {
        assertErrorCount(1, callRules("invalidSwagger.yaml"));
    }

    @Test
    void testInvalidGlobalSwagger() {
        assertErrorCount(1, callRules("invalidGlobalSwagger.yaml"));
    }
}
