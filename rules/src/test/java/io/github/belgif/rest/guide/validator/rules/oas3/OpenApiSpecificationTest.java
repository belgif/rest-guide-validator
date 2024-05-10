package io.github.belgif.rest.guide.validator.rules.oas3;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

@Getter
public class OpenApiSpecificationTest extends AbstractOasRuleTest {
    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        assertViolations(callRules("openapi_bad.yaml"));
    }

}
