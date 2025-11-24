package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class SchemasShouldNotHaveConflictingTypesTest extends AbstractOasRuleTest {

    @Test
    void testAllOfWithConflictingTypes() {
        assertErrorCount(2, callRules("allOfWithConflictingTypes.yaml"));
    }

    @Test
    void testOneOfWithConflictingTypes() {
        assertErrorCount(1, callRules("oneOfWithConflictingTypes.yaml"));
    }

    @Test
    void testAnyOfWithConflictingTypes() {
        assertErrorCount(1, callRules("anyOfWithConflictingTypes.yaml"));
    }

}
