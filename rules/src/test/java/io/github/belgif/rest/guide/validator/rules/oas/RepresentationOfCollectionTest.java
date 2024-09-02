package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Getter
class RepresentationOfCollectionTest extends AbstractOasRuleTest {
    @Test
    void testOpenApiWithout200Response() {
        assertNoViolations(callRules("no200response.yaml"));
    }

    @Test
    void testValidOpenApiWithPostResponse() {
        assertNoViolations(callRules("openapi_with_post_response.yaml"));
    }

    @Test
    void testInvalidOpenApi() {
        OpenApiViolationAggregator aggregator = callRules("openapi_bad.yaml");
        int violationsDoesNotContainItems = (int) aggregator.getActionableViolations().stream().filter(violation -> violation.getMessage().contains("Response does not contain a property 'items' of type array")).count();
        int violationsItemsNotOfTypeObject = (int) aggregator.getActionableViolations().stream().filter(violation -> violation.getMessage().contains("Schema (or subschema for allOf/oneOf/anyOf) of the values in the items array should be of type object")).count();
        assertErrorCount(3, aggregator);
        assertEquals(2, violationsDoesNotContainItems);
        assertEquals(1, violationsItemsNotOfTypeObject);
    }

    @Test
    void testSiblingSchema() {
        assertNoViolations(callRules("siblingSchema.yaml"));
    }

    @Test
    void testInvalidSiblingSchema() {
        assertErrorCount(1, callRules("siblingSchema_bad.yaml"));
    }

    @Test
    void testArrayNotCalledItemsUnderProblemJson() {
        assertErrorCount(1, callRules("arrayNotCalledItemsUnderProblemJson.yaml"));
    }

}
