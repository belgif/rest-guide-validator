package io.github.belgif.rest.styleguide.validation.rules.oas;

import io.github.belgif.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

@Getter
public class GetStatusCodeTest extends AbstractOasRuleTest {
    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        assertErrorCount(6, callRules("swagger_bad.yaml"));
    }
}
