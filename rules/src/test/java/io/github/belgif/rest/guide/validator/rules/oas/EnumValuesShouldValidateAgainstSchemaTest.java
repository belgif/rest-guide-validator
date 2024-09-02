package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumValuesShouldValidateAgainstSchemaTest extends AbstractOasRuleTest {

    @Test
    void nullValueInEnumShouldBeDetected() {
        assertErrorCount(1, callRules("nullValueEnum.yaml"));
    }

    @Test
    void enumOfWrongTypeShouldBeDetected() {
        assertErrorCount(2, callRules("invalidSchemaEnum.yaml"));
    }

    @Test
    void enumOfWrongSimpleTypeShouldBeDetected() {
        assertErrorCount(1, callRules("invalidTypeEnum.yaml"));
    }

    @Test
    void complexEnum() {
        OpenApiViolationAggregator aggregator = callRules("complexEnum.yaml");
        assertErrorCount(1, aggregator);
        assertEquals(2, aggregator.getActionableViolations().get(0).getMessage().lines().count());
    }
}
