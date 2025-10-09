package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class AllOfSchemaTest extends AbstractOasRuleTest {
   @Test
    void testValidOpenApi() {
       assertNoViolations(callRules("openapi.yaml"));
   }

   @Test
    void testDiscriminatorWithoutMapping() {
       assertErrorCount(1, callRules("discriminatorWithoutMapping.yaml"));
   }

   @Test
    void testAllOfWithDoubleProperties() {
       assertErrorCount(10, callRules("allOfWithDoubleProperties.yaml"));
   }

}
