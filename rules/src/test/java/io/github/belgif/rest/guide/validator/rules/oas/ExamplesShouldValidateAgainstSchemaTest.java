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
    }

    @Test
    void testPatterns() {
        ViolationReport aggregator = callRules("doesNotRespectPatterns.yaml");
        assertErrorCount(1, aggregator);
    }

    @Test
    void testEnums() {
        assertErrorCount(1, callRules("notInEnum.yaml"));
    }

    @Test
    void testWrongTypes() {
        ViolationReport aggregator = callRules("wrongType.yaml");
        assertErrorCount(1, aggregator);
    }

    @Test
    void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testMultipleExamplesInComponents() {
        ViolationReport aggregator = callRules("examplesInComponents.yaml");
        assertErrorCount(2, aggregator);
    }

    @Test
    void testMinimumAndMaximumValues() {
        assertErrorCount(3, callRules("minimumAndMaximum.yaml"));
    }

    @Test
    void testDateFormat() {
        ViolationReport aggregator = callRules("dateFormat.yaml");
        assertErrorCount(3, aggregator);
    }

    @Test
    void testDiscriminator() {
        assertErrorCount(4, callRules("discriminator.yaml"));
    }

    @Test
    void testSimpleInvalidDiscriminator() {
        // Changed behavior in networknt validator: a discriminator value that doesn't resolve to a schema is no longer a violation; unless when used when combined with a oneOf
        assertNoViolations(callRules("invalidDiscriminator.yaml"));
    }

    //TODO: add test for discriminator with oneOf, where the discriminator value doesn't resolve to a schema, which should be a violation

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
    void testFailOnOverwrittenDiscriminatorMapping() {
        // New behavior: the networknt validator throws exception when a discriminator mapping is overwritten by schema composition
        assertThrows(IllegalStateException.class, () -> callRules("oneOfOverwrittenDiscriminator/openapi.yaml"));
    }

    @Test
    void testInvalidOneOfDiscriminator() {
        assertErrorCount(1, callRules("oneOfDiscriminatorInvalid/openapi.yaml"));
    }

    @Test
    void testDiscriminatorInSubSchemaOfOneOf() {
        ViolationReport aggregator = callRules("oneOf/openapi.yaml");
        assertErrorCount(1, aggregator);
        assertTrue(aggregator.getActionableViolations().get(0).getMessage().contains("In location /testRequest/schemaUnderTest"));
    }

    @Test
    void testInvalidDiscriminatorInSubSchemaOfOneOf() {
        assertErrorCount(1, callRules("oneOfInvalid/openapi.yaml"));
    }

}
