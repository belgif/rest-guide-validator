package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class DiscriminatorPropertyNameTest extends AbstractOasRuleTest {

    @Test
    void testOneOfDiscriminatorPropertyPresent() {
        assertNoViolations(callRules("oneOfDiscriminatorPropertyPresent.yaml"));
    }

    @Test
    void testOneOfDiscriminatorPropertyAbsent() {
        assertErrorCount(2, callRules("oneOfDiscriminatorPropertyAbsent.yaml"));
    }

}
