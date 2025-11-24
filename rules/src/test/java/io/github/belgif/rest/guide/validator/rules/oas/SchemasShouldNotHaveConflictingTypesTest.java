package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class SchemasShouldNotHaveConflictingTypesTest extends AbstractOasRuleTest {

    @Test
    void testAllOfWithConflictingTypes() {
        assertErrorCount(1, callRules("allOfWithConflictingTypes.yaml"));
    }

}
