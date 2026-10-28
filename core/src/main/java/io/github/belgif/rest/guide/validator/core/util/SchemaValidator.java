package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import io.github.belgif.rest.guide.validator.core.model.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SchemaValidator {

    private SchemaValidator() {
    }

    public static Optional<String> getExampleViolations(ExampleDefinition exampleDefinition) {
        Optional<SchemaDefinition> def = getSchemaDefinition(exampleDefinition);
        if (def.isEmpty()) {
            return Optional.empty();
        }
        SchemaDefinition schemaDefinition = def.get();
        ExampleDefinition example = (ExampleDefinition) exampleDefinition.getResult().resolve(exampleDefinition.getModel());
        return buildViolationString(validateSchema(getExampleNode(example), schemaDefinition));
    }

    public static Set<Map.Entry<String, String>> getEnumViolations(SchemaDefinition schemaDefinition) {
        Set<Map.Entry<String, String>> violations = new HashSet<>();
        SchemaDefinition highLevelSchema = schemaDefinition.getHighLevelSchema();
        for (JsonNode nodeToValidate : getEnumerationNodes(schemaDefinition)) {
            Optional<String> violation = buildViolationString(validateSchema(nodeToValidate, highLevelSchema));
            violation.ifPresent(s -> violations.add(Map.entry(nodeToValidate.toString(), s)));
        }
        return violations;
    }

    public static Optional<String> getDefaultValueViolations(SchemaDefinition schemaDefinition) {
        JsonNode schemaNode = schemaDefinition.getJsonNode();
        if (schemaNode.has("default")) {
            return buildViolationString(validateSchema(schemaNode.get("default"), schemaDefinition));
        }
        return Optional.empty();
    }

    private static JsonNode getExampleNode(ExampleDefinition exampleDefinition) {
        JsonNode exampleNode = exampleDefinition.getJsonNode();
        if (exampleDefinition.isOasExampleObject() && exampleNode.has("value")) {
            exampleNode = exampleNode.get("value");
        }
        return exampleNode;
    }

    private static Set<JsonNode> getEnumerationNodes(SchemaDefinition schemaDefinition) {
        Set<JsonNode> enumerations = new HashSet<>();
        JsonNode schema = schemaDefinition.getJsonNode();
        for (JsonNode enumeration : schema.get("enum")) {
            enumerations.add(enumeration);
        }
        return enumerations;
    }

    private static Set<String> validateSchema(JsonNode instanceNode, SchemaDefinition schemaDefinition) {
        try {
            Map<String, String> documents = schemaDefinition.getResult().getSrc().values().stream()
                    .collect(Collectors.toMap(
                            source -> source.getFile().getAbsoluteFile().toURI().toString(),
                            source -> source.getJsonNode().toString(),
                            (first, ignored) -> first,
                            HashMap::new));
            var registry = NetworkntSchemaValidation.createRegistry(documents);
            String schemaUri = schemaDefinition.getOpenApiFile().getAbsoluteFile().toURI().toString()
                    + "#" + schemaDefinition.getJsonPointer().getJsonPointer();
            Schema schema = NetworkntSchemaValidation.schema(registry, schemaUri);
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
        String schemaLocation = error.getSchemaLocation().toString();
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = "Validation failed for keyword '" + error.getKeyword() + "'";
        }
        return "Instance " + (instanceLocation.isEmpty() ? "/" : instanceLocation) + ": " + message
                + " In Schema: " + schemaDefinition.getOpenApiFile().getName() + "#"
                + schemaDefinition.getJsonPointer() + " : " + schemaLocation;
    }

    private static Optional<SchemaDefinition> getSchemaDefinition(ExampleDefinition exampleDefinition) {
        OpenApiDefinition<?> parentDef = exampleDefinition.getParent();
        org.eclipse.microprofile.openapi.models.media.Schema schema;
        if (parentDef instanceof SchemaDefinition schemaDefinition) {
            schema = schemaDefinition.getModel();
        } else if (parentDef instanceof MediaTypeDefinition mediaTypeDefinition) {
            schema = mediaTypeDefinition.getModel().getSchema();
        } else if (parentDef instanceof ParameterDefinition parameterDefinition) {
            schema = parameterDefinition.getModel().getSchema();
        } else if (parentDef instanceof ResponseHeaderDefinition responseHeaderDefinition) {
            schema = responseHeaderDefinition.getModel().getSchema();
        } else {
            throw new RuntimeException("[Internal Error] Unable to find schema related to example: " + exampleDefinition.getJsonPointer());
        }
        return schema != null ? Optional.of((SchemaDefinition) exampleDefinition.getResult().resolve(schema)) : Optional.empty();
    }

    private static Optional<String> buildViolationString(Set<String> violations) {
        if (violations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join("\n", violations));
    }
}
