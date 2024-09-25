package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
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
        ViolationReport aggregator = callRules("swagger_bad.yaml");
        int pathSegmentViolations = (int) aggregator.getActionableViolations().stream().filter(violation -> violation.getDescription().contains("Path segments")).count();
        int pathParamViolations = (int) aggregator.getActionableViolations().stream().filter(violation -> violation.getDescription().contains("Path parameters")).count();
        int queryParamViolations = (int) aggregator.getActionableViolations().stream().filter(violation -> violation.getDescription().contains("Query parameters")).count();
        assertEquals(1, pathSegmentViolations);
        assertEquals(1, pathParamViolations);
        assertEquals(1, queryParamViolations);
        assertErrorCount(3, aggregator);
    }
}
