package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class SchemasShouldNotHaveConflictingTypesTest extends AbstractOasRuleTest {

    @Test
    void testConflictingAllOf() {
        assertErrorCount(1, callRules("conflictingAllOf.yaml"));
    }

    @Test
    void testValidOneOf() {
        assertNoViolations(callRules("validOneOf.yaml"));
    }

    @Test
    void testValidAllOf() {
        assertNoViolations(callRules("validAllOf.yaml"));
    }

    @Test
    void testConflictingOneOfWithMainType() {
        assertErrorCount(1, callRules("conflictingOneOfWithMainType.yaml"));
    }

    @Test
    void testConflictingAnyOfWithMainType() {
        assertErrorCount(1, callRules("conflictingAnyOfWithMainType.yaml"));
    }

    @Test
    void testAllOfComplicated() {
        assertNoViolations(callRules("allOfComplicated.yaml"));
    }

    @Test
    void testConflictingAnyOf() {
        assertErrorCount(1, callRules("conflictingAnyOf.yaml"));
    }

    @Test
    void testValidOneOfWithTypeDeclarations() {
        assertNoViolations(callRules("validOneOfWithTypeDeclarations.yaml"));
    }

}
