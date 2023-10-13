package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

@Getter
public class PostStatusCodeTest extends AbstractOasRuleTest {

    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        assertViolations(callRules("swagger_bad.yaml"));
    }

}
