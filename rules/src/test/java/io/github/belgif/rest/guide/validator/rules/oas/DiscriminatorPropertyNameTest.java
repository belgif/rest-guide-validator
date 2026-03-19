package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class DiscriminatorPropertyNameTest extends AbstractOasRuleTest {

    @Test
    void testOneOfDiscriminatorPropertyPresent() {
        assertNoViolations(callRules("oneOfDiscriminatorPropertyPresent.yaml"));
    }

    @Test
    void testComplexOneOfDiscriminatorPropertyPresent() {
        assertNoViolations(callRules("complexOneOfDiscriminatorPropertyPresent.yaml"));
    }

    @Test
    void testOneOfDiscriminatorPropertyAbsent() {
        assertErrorCount(2, callRules("oneOfDiscriminatorPropertyAbsent.yaml"));
    }

    @Test
    void testOneOfDiscriminatorPropertyNotRequired() {
        assertErrorCount(1, callRules("oneOfDiscriminatorPropertyNotRequired.yaml"));
    }

    @Test
    void testAllOfDiscriminatorPropertyPresent() {
        assertNoViolations(callRules("allOfDiscriminatorPropertyPresent.yaml"));
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
