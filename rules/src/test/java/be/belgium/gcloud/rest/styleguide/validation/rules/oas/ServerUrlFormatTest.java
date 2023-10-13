package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class ServerUrlFormatTest extends AbstractOasRuleTest {
    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        assertViolations(callRules("swagger_bad.yaml"));
    }

    @Test
    public void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testInvalidOpenApi() {
        assertViolations(callRules("openapi_bad.yaml"));
    }

}
