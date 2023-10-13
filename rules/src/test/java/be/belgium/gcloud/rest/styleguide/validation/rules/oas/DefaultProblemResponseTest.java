package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class DefaultProblemResponseTest extends AbstractOasRuleTest {
    @Test
    public void testValid() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

    @Test
    public void testInvalidOpenApi() {
        assertErrorCount(8, callRules("openapi_bad.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        assertErrorCount(4, callRules("swagger_bad.yaml"));
    }
}
