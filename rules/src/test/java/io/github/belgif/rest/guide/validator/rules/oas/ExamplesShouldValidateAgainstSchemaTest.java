package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamplesShouldValidateAgainstSchemaTest extends AbstractOasRuleTest {

    @Test
    void testRequiredValue() {
        assertErrorCount(1, callRules("requiredValue.yaml"));
    }

    @Test
    void testPatterns() {
        assertErrorCount(1, callRules("doesNotRespectPatterns.yaml"));
    }

    @Test
    void testEnums() {
        assertErrorCount(1, callRules("notInEnum.yaml"));
    }

    @Test
    void testWrongTypes() {
        assertErrorCount(1, callRules("wrongType.yaml"));
    }

    @Test
    void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testMultipleExamplesInComponents() {
        assertErrorCount(2, callRules("examplesInComponents.yaml"));
    }

    @Test
    void testMinimumAndMaximumValues() {
        assertErrorCount(3, callRules("minimumAndMaximum.yaml"));
    }

    @Test
    void testDateFormat() {
        assertErrorCount(3, callRules("dateFormat.yaml"));
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

}
