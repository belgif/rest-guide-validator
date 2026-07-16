package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class ReferencesShouldPointToTopLevelDefinitionsTest extends AbstractOasRuleTest {

    @Test
    void testReferenceToTopLevelDefinition() {
        assertNoViolations(callRules("refToTopLevelDefinition.yaml"));
    }

    @Test
    void testReferenceToInlineDefinition() {
        assertErrorCount(1, callRules("refToInlineDefinition.yaml"));
    }

    @Test
    void testDiscriminatorMappingToTopLevelDefinition() {
        assertNoViolations(callRules("discriminatorMappingToTopLevelDefinition.yaml"));
    }

    @Test
    void testDiscriminatorMappingToInlineDefinition() {
        assertErrorCount(1, callRules("discriminatorMappingToInlineDefinition.yaml"));
    }
}
