package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.openapitools.empoa.swagger.core.internal.models.examples.SwExample;

public class ExampleMapper {

    private ExampleMapper() {
    }

    /*
    OpenApi contains $ref, and model class contains ref, so ObjectMapper doesn't work by default.
     */
    public static SwExample mapToExampleObject(JsonNode exampleNode) {
        var summary = exampleNode.has("summary") ? exampleNode.get("summary").asText() : null;
        var description = exampleNode.has("description") ? exampleNode.get("description").asText() : null;
        var value = exampleNode.has("value") ? exampleNode.get("value") : null;
        var ref = exampleNode.has("$ref") ? exampleNode.get("$ref").asText() : null;
        var exampleObject = new SwExample();
        exampleObject.setValue(value);
        exampleObject.setSummary(summary);
        exampleObject.setDescription(description);
        exampleObject.setRef(ref);
        return exampleObject;
    }

}
