package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

@Getter
public class DeleteStatusCodeTest extends AbstractOasRuleTest {
    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }

    @Test
    public void testInvalidSwagger() {
        assertErrorCount(1, callRules("swagger_bad.yaml"));
    }

}
