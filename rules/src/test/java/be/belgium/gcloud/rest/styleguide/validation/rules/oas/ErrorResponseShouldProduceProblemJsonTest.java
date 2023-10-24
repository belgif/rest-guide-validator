package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class ErrorResponseShouldProduceProblemJsonTest extends AbstractOasRuleTest {
    @Test
    public void testValid() {
        assertNoViolations(callRules("openapi.yaml"));
    }
    @Test
    public void testInvalid() {
        assertErrorCount(5, callRules("openapi_bad.yaml"));
    }
}
