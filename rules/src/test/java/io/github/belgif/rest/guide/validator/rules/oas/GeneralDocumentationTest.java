package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class GeneralDocumentationTest extends AbstractOasRuleTest {


    @Test
    void testOperationSummaryPresent() {
        assertNoViolations(callRules("summaryPresent.yaml"));
    }

    @Test
    void testOperationSummaryNotPresent() {
        assertErrorCount(3, callRules("summaryNotPresent.yaml"));
    }

    @Test
    void testInlineSchemaTitlePresent() {
        assertNoViolations(callRules("inlineSchemaTitlePresent.yaml"));
    }

    @Test
    void testInlineSchemaTitleNotUpperCamelCase() {
        assertErrorCount(3, callRules("inlineSchemaTitleNotUpperCamelCase.yaml"));
    }

    @Test
    void testInlineSchemaTitleNotPresent() {
        assertErrorCount(1, callRules("inlineSchemaTitleNotPresent.yaml"));
    }

    @Test
    void testInlineSchemaTitleAbsentInAllOf() {
        assertNoViolations(callRules("inlineSchemaTitleNotPresentInAllOf.yaml"));
    }

    @Test
    void testComponentSchemaTitleNotPresent() {
        assertNoViolations(callRules("titleNotPresent.yaml"));
    }

    @Test
    void testComponentSchemaTitleMatchesSchemaName() {
        assertNoViolations(callRules("titleMatchesSchemaName.yaml"));
    }

    @Test
    void testComponentSchemaTitleDoesNotMatchesSchemaName() {
        assertErrorCount(1, callRules("titleDoesNotMatchesSchemaName.yaml"));
    }

    @Test
    void testComponentSchemaTitlePresentInNonObjectInlineSchema() {
        assertErrorCount(5, callRules("titlePresentInNonObjectInlineSchema.yaml"));
    }
}



