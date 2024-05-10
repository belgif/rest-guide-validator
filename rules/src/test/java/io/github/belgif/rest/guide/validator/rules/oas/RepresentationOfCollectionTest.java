package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.api.Test;

@Getter
public class RepresentationOfCollectionTest extends AbstractOasRuleTest {
    @Test
    public void testValidSwagger() {
        assertNoViolations(callRules("swagger.yaml"));
    }
    @Test
    public void testValidOpenApiWithPostResponse() {
        assertNoViolations(callRules("openapi_with_post_response.yaml"));
    }
    @Test
    public void testInvalidSwagger() {
        assertErrorCount(4, callRules("swagger_bad.yaml"));
    }
    @Test
    public void testInvalidOpenApi() {
        assertErrorCount(3, callRules("openapi_bad.yaml"));
    }
}
