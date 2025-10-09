package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class AllOfSchemaTest extends AbstractOasRuleTest {
   @Test
    void testValidOpenApi() {
       assertNoViolations(callRules("discriminatorWithMapping.yaml"));
   }

   @Test
    void testInvalidOpenApi() {
       assertErrorCount(1, callRules("discriminatorWithoutMapping.yaml"));
   }

}
