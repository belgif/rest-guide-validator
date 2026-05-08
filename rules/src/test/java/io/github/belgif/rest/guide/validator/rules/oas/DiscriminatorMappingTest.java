package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class DiscriminatorMappingTest extends AbstractOasRuleTest {

    @Test
    void testDiscriminatorWithImplicitMapping() {
        assertErrorCount(2, callRules("discriminatorWithImplicitMapping.yaml"));
    }

    @Test
    void testDiscriminatorWithoutMapping() {
        assertErrorCount(1, callRules("discriminatorWithoutMapping.yaml"));
    }
}
