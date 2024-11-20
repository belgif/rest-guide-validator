package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamplesShouldValidateAgainstSchemaTest extends AbstractOasRuleTest {

    @Test
    void testRequiredValue() {
        ViolationReport aggregator = callRules("requiredValue.yaml");
        assertErrorCount(1, aggregator);
        assertEquals(6, aggregator.getActionableViolations().get(0).getMessage().lines().count());
    }

    @Test
    void testPatterns() {
        ViolationReport aggregator = callRules("doesNotRespectPatterns.yaml");
        assertErrorCount(1, aggregator);
        assertEquals(2, aggregator.getActionableViolations().get(0).getMessage().lines().count());
    }

    @Test
    void testEnums() {
        assertErrorCount(1, callRules("notInEnum.yaml"));
    }

    @Test
    void testWrongTypes() {
        ViolationReport aggregator = callRules("wrongType.yaml");
        assertErrorCount(1, aggregator);
        assertEquals(2, aggregator.getActionableViolations().get(0).getMessage().lines().count());
    }

    @Test
    void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testMultipleExamplesInComponents() {
        ViolationReport aggregator = callRules("examplesInComponents.yaml");
        assertErrorCount(2, aggregator);
        assertEquals(2, aggregator.getActionableViolations().get(0).getMessage().lines().count());
        assertEquals(2, aggregator.getActionableViolations().get(1).getMessage().lines().count());
    }

    @Test
    void testMinimumAndMaximumValues() {
        assertErrorCount(3, callRules("minimumAndMaximum.yaml"));
    }

    @Test
    void testDateFormat() {
        ViolationReport aggregator = callRules("dateFormat.yaml");
        assertErrorCount(3, aggregator);
        assertEquals(2, aggregator.getActionableViolations().get(1).getMessage().lines().count());
        assertEquals(2, aggregator.getActionableViolations().get(2).getMessage().lines().count());
    }

    @Test
    void testDiscriminator() {
        assertErrorCount(2, callRules("discriminator.yaml"));
    }

    @Test
    void testUnrelatedDiscriminator() {
        ViolationReport aggregator = callRules("unrelatedDiscriminator.yaml");
        assertErrorCount(1, aggregator);
        assertEquals("href: Value '/invalidUrl' does not match format 'uri'. In Schema: unrelatedDiscriminator.yaml#/components/schemas/NotificationsCollection : <allOf>.<format>", aggregator.getActionableViolations().get(0).getMessage());
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
        assertErrorCount(1, callRules("discriminatorRefs/failingDiscriminator.yaml"));
    }

}
