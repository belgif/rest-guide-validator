package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Getter
public class PathCamelCaseTest extends AbstractOasRuleTest {
    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        OpenApiViolationAggregator aggregator = callRules("swagger_bad.yaml");
        int pathSegmentViolations = (int) aggregator.getViolations().stream().filter(violation -> violation.getMessage().contains("Path segments")).count();
        int pathParamViolations = (int) aggregator.getViolations().stream().filter(violation -> violation.getMessage().contains("Path parameters")).count();
        int queryParamViolations = (int) aggregator.getViolations().stream().filter(violation -> violation.getMessage().contains("Query parameters")).count();
        assertEquals(3, pathSegmentViolations);
        assertEquals(1, pathParamViolations);
        assertEquals(3, queryParamViolations);
        assertErrorCount(7, aggregator);
    }
}
