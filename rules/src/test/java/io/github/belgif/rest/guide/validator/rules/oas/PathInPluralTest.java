package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

@Getter
class PathInPluralTest extends AbstractOasRuleTest {
    @Test
    void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testInvalidOpenApi() {
        assertErrorCount(1, callRules("openapi_bad.yaml"));
    }

    @Test
    void historyAllowedExceptionTest() {
        assertNoViolations(callRules("historyAllowed.yaml"));
    }

    @Test
    void testSiblingSchema() {
        assertErrorCount(1, callRules("siblingSchema.yaml"));
    }

    @Test
    void testReferencesAreFollowed() {
        assertNoViolations(callRules("referencesFollowed.yaml"));
    }

    @Test
    void testResponseWithoutPayload() {
        assertNoViolations(callRules("responseWithoutPayload.yaml"));
    }
}
