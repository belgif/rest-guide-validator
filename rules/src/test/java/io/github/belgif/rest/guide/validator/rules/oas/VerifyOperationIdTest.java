package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifyOperationIdTest extends AbstractOasRuleTest {
   @Test
    void testValidOpenApi() {
       assertNoViolations(callRules("openapi.yaml"));
   }

   @Test
    void testOperationIdNotSpecified() {
       ViolationReport violationReport = callRules("operationIdNotSpecified.yaml");
       assertErrorCount(1, violationReport);
       assertEquals("OperationId not specified.", violationReport.getViolations().get(0).getMessage());
   }

    @Test
    void testOperationIdNotInLowerCamelCase() {
        ViolationReport violationReport = callRules("operationIdNotInLowerCamelCase.yaml");
        assertErrorCount(1, violationReport);
        assertEquals("OperationId not lowerCamelCase: MyPath", violationReport.getViolations().get(0).getMessage());
    }

    @Test
    void testDuplicateOperationId() {
        ViolationReport violationReport = callRules("duplicateOperationId.yaml");
        assertErrorCount(1, violationReport);
        assertTrue(violationReport.getViolations().get(0).getMessage().contains("OperationId not unique, present in: "));
    }

    @Test
    void testDuplicateOperationIdInReferencedFileShouldBeIgnored() {
       assertNoViolations(callRules("referencedOperationIdNotIncludedInRuleMain.yaml"));
    }

    @Test
    void testDuplicateOperationIdInReferencedFile() {
       assertErrorCount(1, callRules("referencedOperationIdShouldBeIncludedInRule.yaml"));
    }
}
