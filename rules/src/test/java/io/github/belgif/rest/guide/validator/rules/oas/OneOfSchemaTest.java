package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class OneOfSchemaTest extends AbstractOasRuleTest {

   @Test
    void testOneOfWithRequiredConstraint() {
       assertNoViolations(callRules("oneOfWithRequiredConstraint.yaml"));
   }

   @Test
    void testOneOfWithFullObjectConstraint() {
       assertErrorCount(2, callRules("oneOfWithFullObjectConstraint.yaml"));
   }

   @Test
    void testOneOfWithEnumConstraint() {
       assertNoViolations(callRules("oneOfWithEnumConstraint.yaml"));
   }
}
