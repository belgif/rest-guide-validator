package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class ExamplesShouldValidateAgainstSchemaTest extends AbstractOasRuleTest {

    @Test
    public void testRequiredValue() {
        assertErrorCount(2, callRules("requiredValue.yaml"));
    }

    @Test
    public void testPatterns() {
        assertErrorCount(2, callRules("doesNotRespectPatterns.yaml"));
    }

    @Test
    public void testEnums() {
        assertErrorCount(1, callRules("notInEnum.yaml"));
    }

    @Test
    public void testWrongTypes() {
        assertErrorCount(2, callRules("wrongType.yaml"));
    }

    @Test
    public void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testMultipleExamplesInComponents() {
        assertErrorCount(4, callRules("examplesInComponents.yaml"));
    }

    @Test
    public void testMinimumAndMaximumValues() {
        assertErrorCount(3, callRules("minimumAndMaximum.yaml"));
    }

    @Test
    public void testDateFormat() {
        assertErrorCount(4, callRules("dateFormat.yaml"));
    }

    @Test
    public void testDiscriminator() {
        assertErrorCount(2, callRules("discriminator.yaml"));
    }

}
