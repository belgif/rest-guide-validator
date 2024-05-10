package io.github.belgif.rest.styleguide.validation.rules.oas;

import io.github.belgif.rest.styleguide.validation.core.OpenApiViolationAggregator;
import io.github.belgif.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Getter
public class TagGuidelinesTest extends AbstractOasRuleTest {
    @Test
    public void testValidOpenapi() {
        assertNoViolations(callRules("openapi.yaml"));
    }
    @Test
    public void testInvalidOpenApi() {
        OpenApiViolationAggregator aggregator = callRules("openapi_bad.yaml");
        int capitalLetterViolations = (int) aggregator.getViolations().stream().filter(violation -> violation.getMessage().contains("capital letter")).count();
        int multipleTagsViolations = (int) aggregator.getViolations().stream().filter(violation -> violation.getMessage().contains("more than one tag")).count();
        int nonDeclaredViolations = (int) aggregator.getViolations().stream().filter(violation -> violation.getMessage().contains("used but not declared")).count();
        assertEquals(1, capitalLetterViolations);
        assertEquals(1, multipleTagsViolations);
        assertEquals(1, nonDeclaredViolations);
        assertErrorCount(3, aggregator);
    }
}
