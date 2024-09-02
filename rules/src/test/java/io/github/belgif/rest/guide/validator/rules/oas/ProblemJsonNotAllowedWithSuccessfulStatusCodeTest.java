package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

@Getter
class ProblemJsonNotAllowedWithSuccessfulStatusCodeTest extends AbstractOasRuleTest {
    @Test
    void testValidOpenapi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testInvalidOpenapi() {
        assertErrorCount(3, callRules("problemJsonResponses.yaml"));
    }

    @Test
    void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

}
