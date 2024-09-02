package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagGuidelinesTest extends AbstractOasRuleTest {
    @Test
    void testValidOpenapi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testInvalidOpenApi() {
        OpenApiViolationAggregator aggregator = callRules("openapi_bad.yaml");
        int capitalLetterViolations = (int) aggregator.getActionableViolations().stream().filter(violation -> violation.getDescription().contains("capital letter")).count();
        int multipleTagsViolations = (int) aggregator.getActionableViolations().stream().filter(violation -> violation.getDescription().contains("more than one tag")).count();
        int nonDeclaredViolations = (int) aggregator.getActionableViolations().stream().filter(violation -> violation.getMessage() != null && violation.getMessage().contains("used but not declared")).count();
        assertEquals(1, capitalLetterViolations);
        assertEquals(1, multipleTagsViolations);
        assertEquals(1, nonDeclaredViolations);
        assertErrorCount(3, aggregator);
    }
}
