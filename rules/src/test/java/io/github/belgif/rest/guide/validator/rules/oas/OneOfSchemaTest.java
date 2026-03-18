package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OneOfSchemaTest extends AbstractOasRuleTest {

   @Test
    void testOneOfWithRequiredConstraint() {
       assertNoViolations(callRules("oneOfWithRequiredConstraint.yaml"));
   }

   @Test
    void testOneOfWithFullObjectConstraint() {
       assertErrorCount(1, callRules("oneOfWithFullObjectConstraint.yaml"));
   }

   @Test
    void testOneOfWithEnumConstraint() {
       assertNoViolations(callRules("oneOfWithEnumConstraint.yaml"));
   }

    @Test
    void testOneOfWithEnumAndDifferentTypeConstraint() {
        assertErrorCount(2, callRules("oneOfWithEnumAndDifferentTypeConstraint.yaml"));
    }

    @Test
    void testOneOfWithMultipleObjectTypes() {
       assertErrorCount(1, callRules("oneOfWithMultipleObjectTypes.yaml"));
    }

    @Test
    void testSubSchemasWithTypeObjectAndDiscriminator() {
       assertNoViolations(callRules("subSchemasWithTypeObject.yaml"));
    }

    @Test
    void testSubSchemasWithDiscriminatorInParent() {
       assertErrorCount(1, callRules("subSchemasWithDiscriminatorInParent.yaml"));
    }

    @Test
    void testOneOfWithExtraProperty() {
       assertErrorCount(1, callRules("oneOfWithExtraProperty.yaml"));
    }

    @Test
    void testOneOfWithoutDiscriminator() {
        assertErrorCount(1, callRules("oneOfWithoutDiscriminator.yaml"));
    }

    @Test
    void testNonCombinableSubSchemas() {
       ViolationReport report = callRules("nonCombinableOneOfSchemas.yaml");
       assertErrorCount(1, report);
       assertEquals("All subschemas should comply with one of the allowed uses for a oneOf schema.", report.getViolations().get(0).getMessage());
    }
}
