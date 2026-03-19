package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class AllOfSchemaTest extends AbstractOasRuleTest {
    @Test
    void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testDiscriminatorWithoutMapping() {
        assertErrorCount(1, callRules("discriminatorWithoutMapping.yaml"));
    }

    @Test
    void testAllOfWithDoubleProperties() {
        assertErrorCount(2, callRules("allOfWithDoubleProperties.yaml"));
    }

    @Test
    void testAllOfDiscriminatorPropertyPresent() {
        assertNoViolations(callRules("allOfDiscriminatorPropertyPresent.yaml"));
    }

    @Test
    void testComplexAllOfDiscriminatorPropertyPresent() {
        assertNoViolations(callRules("complexAllOfDiscriminatorPropertyPresent.yaml"));
    }

    @Test
    void testAllOfDiscriminatorPropertyAbsent() {
        assertErrorCount(1, callRules("allOfDiscriminatorPropertyAbsent.yaml"));
    }

    @Test
    void testAllOfDiscriminatorPropertyNotRequired() {
        assertErrorCount(1, callRules("allOfDiscriminatorPropertyNotRequired.yaml"));
    }

}
