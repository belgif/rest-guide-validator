package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SchemaValidator {

    private SchemaValidator() {
    }

    public static Set<String> validateSchema(JsonNode instanceNode, SchemaDefinition schemaDefinition) {
        try {
            // TODO: consider creating/parsing schema registry only once per run to improve performance. Cache key could be the entry OpenAPI document
            Map<String, String> documents = schemaDefinition.getResult().getSrc().values().stream()
                    .collect(Collectors.toMap(
                            source -> source.getFile().getAbsoluteFile().toURI().toString(),
                            source -> source.getJsonNode().toString(),
                            (first, ignored) -> first,
                            HashMap::new));
            var registry = NetworkntSchemaRegistryBuilder.createRegistry(documents);
            String schemaUri = schemaDefinition.getOpenApiFile().getAbsoluteFile().toURI()
                    + "#" + schemaDefinition.getJsonPointer().getJsonPointer();
            Schema schema = registry.getSchema(SchemaLocation.of(schemaUri));
            return schema.validate(instanceNode).stream()
                    .map(error -> formatViolation(error, schemaDefinition))
                    .collect(Collectors.toSet());
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    schemaDefinition.getOpenApiFile().getName() + "#" + schemaDefinition.getJsonPointer()
                            + ": Unable to validate schema instance",
                    exception);
        }
    }

    private static String formatViolation(Error error, SchemaDefinition schemaDefinition) {
        String instanceLocation = error.getInstanceLocation().toString();
        String schemaLocation = error.getSchemaLocation().toString();  // TODO: schemaLocation includes full file:/... absolute IRI, could be reduced to file name for brevity
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = "Validation failed for keyword '" + error.getKeyword() + "'";
        }
        return "Value at " + (instanceLocation.isEmpty() ? "/" : instanceLocation) + ": " + message
                + " Validated by: " + schemaLocation + " via schema " + schemaDefinition.getOpenApiFile().getName() + "#"
                + schemaDefinition.getJsonPointer();
    }

}
