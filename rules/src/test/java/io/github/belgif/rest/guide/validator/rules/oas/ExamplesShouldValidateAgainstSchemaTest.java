package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamplesShouldValidateAgainstSchemaTest extends AbstractOasRuleTest {

    @Test
    void testRequiredValue() {
        ViolationReport aggregator = callRules("requiredValue.yaml");
        assertErrorCount(1, aggregator);
        assertTrue(aggregator.getActionableViolations().get(0).getMessage().contains("Instance "));
    }

    @Test
    void testPatterns() {
        ViolationReport aggregator = callRules("doesNotRespectPatterns.yaml");
        assertErrorCount(1, aggregator);
        assertTrue(aggregator.getActionableViolations().get(0).getMessage().contains("Instance "));
    }

    @Test
    void testEnums() {
        assertErrorCount(1, callRules("notInEnum.yaml"));
    }

    @Test
    void testWrongTypes() {
        ViolationReport aggregator = callRules("wrongType.yaml");
        assertErrorCount(1, aggregator);
        assertTrue(aggregator.getActionableViolations().get(0).getMessage().contains("Instance "));
    }

    @Test
    void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testMultipleExamplesInComponents() {
        ViolationReport aggregator = callRules("examplesInComponents.yaml");
        assertErrorCount(2, aggregator);
        assertTrue(aggregator.getActionableViolations().stream()
                .allMatch(violation -> violation.getMessage().contains("Instance ")));
    }

    @Test
    void testMinimumAndMaximumValues() {
        assertErrorCount(3, callRules("minimumAndMaximum.yaml"));
    }

    @Test
    void testDateFormat() {
        ViolationReport aggregator = callRules("dateFormat.yaml");
        assertErrorCount(3, aggregator);
        assertTrue(aggregator.getActionableViolations().stream()
                .allMatch(violation -> violation.getMessage().contains("Instance ")));
    }

    @Test
    void testDiscriminator() {
        assertErrorCount(4, callRules("discriminator.yaml"));
    }

    @Test
    void testSimpleInvalidDiscriminator() {
        assertNoViolations(callRules("invalidDiscriminator.yaml"));
    }

    @Test
    void testUnrelatedDiscriminator() {
        ViolationReport aggregator = callRules("unrelatedDiscriminator.yaml");
        assertErrorCount(1, aggregator);
        String message = aggregator.getActionableViolations().get(0).getMessage();
        assertTrue(message.contains("Instance /href"));
        assertTrue(message.contains("unrelatedDiscriminator.yaml#/components/schemas/NotificationsCollection"));
    }

    @Test
    void testSchemaInExternalFile() {
        assertErrorCount(1, callRules("schemaInExternalFile.yaml"));
    }

    @Test
    void testDiscriminatorRefs() {
        assertNoViolations(callRules("discriminatorRefs/discriminator.yaml"));
    }

    @Test
    void testComplexDiscriminatorRefs() {
        assertNoViolations(callRules("discriminatorRefs/complexDiscriminator.yaml"));
    }

    @Test
    void testFailingDiscriminatorRefs() {
        assertThrows(RuntimeException.class, () -> callRules("discriminatorRefs/failingDiscriminator.yaml"));
    }

    @Test
    void testFailingDiscriminatorsInResponseObject() {
        assertNoViolations(callRules("discriminatorRefsInResponse/openapi.yaml"));
    }

    @Test
    void testOneOfDiscriminator() {
        assertThrows(IllegalStateException.class, () -> callRules("oneOfDiscriminator/openapi.yaml"));
    }

    @Test
    void testInvalidOneOfDiscriminator() {
        assertErrorCount(1, callRules("oneOfDiscriminatorInvalid/openapi.yaml"));
    }

    @Test
    void testDiscriminatorInSubSchemaOfOneOf() {
        ViolationReport aggregator = callRules("oneOf/openapi.yaml");
        assertErrorCount(1, aggregator);
        assertTrue(aggregator.getActionableViolations().get(0).getMessage().contains("Instance /testRequest/schemaUnderTest"));
    }

    @Test
    void testInvalidDiscriminatorInSubSchemaOfOneOf() {
        assertErrorCount(1, callRules("oneOfInvalid/openapi.yaml"));
    }

}
