package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class GeneralDocumentationTest extends AbstractOasRuleTest {


    @Test
    void testOperationSummaryPresent() {
        assertNoViolations(callRules("summaryPresent.yaml"));
    }

    @Test
    void testOperationSummaryNotPresent() {
        ViolationReport violationReport = callRules("summaryNotPresent.yaml");
        assertErrorCount(3, violationReport);
    }

    @Test
    void testInlineSchemaTitlePresent() {
        assertNoViolations(callRules("inlineSchemaTitlePresent.yaml"));
    }

    @Test
    void testInlineSchemaTitleNotPresent() {
        ViolationReport violationReport = callRules("inlineSchemaTitleNotPresent.yaml");
        assertErrorCount(3, violationReport);
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
    void testComponentSchemaTitlePresent() {
        ViolationReport violationReport = callRules("titlePresentInCompentSchema.yaml");
        assertErrorCount(1, violationReport);
    }

    @Test
    void testComponentSchemaTitlePresentInInlineArraySchema() {
        ViolationReport violationReport = callRules("titlePresentInInlineArraySchema.yaml");
        assertErrorCount(1, violationReport);
    }
}



