package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.belgif.rest.guide.validator.core.model.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SchemaValidationSupport {

    private SchemaValidationSupport() {
    }

    public static Optional<String> getExampleViolations(ExampleDefinition exampleDefinition) {
        Optional<SchemaDefinition> def = getSchemaDefinition(exampleDefinition);
        if (def.isEmpty()) {
            return Optional.empty();
        }
        SchemaDefinition schemaDefinition = def.get();
        ExampleDefinition example = (ExampleDefinition) exampleDefinition.getResult().resolve(exampleDefinition.getModel());
        return buildViolationString(SchemaValidator.validateSchema(getExampleNode(example), schemaDefinition));
    }

    public static Set<Map.Entry<String, String>> getEnumViolations(SchemaDefinition schemaDefinition) {
        Set<Map.Entry<String, String>> violations = new HashSet<>();
        SchemaDefinition highLevelSchema = schemaDefinition.getHighLevelSchema();
        for (JsonNode nodeToValidate : getEnumerationNodes(schemaDefinition)) {
            Optional<String> violation = buildViolationString(SchemaValidator.validateSchema(nodeToValidate, highLevelSchema));
            violation.ifPresent(s -> violations.add(Map.entry(nodeToValidate.toString(), s)));
        }
        return violations;
    }

    public static Optional<String> getDefaultValueViolations(SchemaDefinition schemaDefinition) {
        JsonNode schemaNode = schemaDefinition.getJsonNode();
        if (schemaNode.has("default")) {
            return buildViolationString(SchemaValidator.validateSchema(schemaNode.get("default"), schemaDefinition));
        }
        return Optional.empty();
    }

    // Internal methods

    /**
     * @param exampleDefinition The ExampleDefinition object to be transformed into a JsonNode.
     * @return JsonNode of the original value of the example.
     * <p>
     * <br />
     * Using exampleDefinition.getModel().getValue() doesn't work because swagger-parser transforms some of these example values
     * into objects (i.e. Date or DateTime), so the original value is lost in those cases.
     * Using the original value from the openapi file ensures a correct validation of the example.
     * </p>
     */
    private static JsonNode getExampleNode(ExampleDefinition exampleDefinition) {
        JsonNode exampleNode = exampleDefinition.getJsonNode();
        if (exampleDefinition.isOasExampleObject() && exampleNode.has("value")) {
            exampleNode = exampleNode.get("value");
        }
        return exampleNode;
    }

    /**
     * @param schemaDefinition The SchemaDefinition object where the enumerations have to be fetched from
     * @return Set<JsonNode> of the original values of the enumerations.
     * <p><br />
     * Using schemaDefinition.getModel().getEnumeration() doesn't work because swagger-parser tries to transform these values
     * into the object type of the schema. When this doesn't match it has a null value and the original value is lost.
     * Using the original value from the openapi file ensures a correct validation of the enumeration.
     * </p>
     */
    private static Set<JsonNode> getEnumerationNodes(SchemaDefinition schemaDefinition) {
        Set<JsonNode> enumerations = new HashSet<>();
        JsonNode schema = schemaDefinition.getJsonNode();
        for (JsonNode enumeration : schema.get("enum")) {
            enumerations.add(enumeration);
        }
        return enumerations;
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
