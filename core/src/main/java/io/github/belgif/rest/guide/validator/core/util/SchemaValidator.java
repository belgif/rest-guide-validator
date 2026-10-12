package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SchemaException;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.path.NodePath;
import io.github.belgif.rest.guide.validator.core.model.*;
import io.github.belgif.rest.guide.validator.core.model.helper.SchemaValidationContext;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SchemaValidator {

    private SchemaValidator() {
    }

    public static Optional<String> getExampleViolations(ExampleDefinition exampleDefinition) {
        try {
            Optional<SchemaDefinition> def = getSchemaDefinition(exampleDefinition);
            if (def.isEmpty()) {
                return Optional.empty();
            }
            SchemaDefinition schemaDefinition = def.get();
            ExampleDefinition example = (ExampleDefinition) exampleDefinition.getResult().resolve(exampleDefinition.getModel());
            JsonNode exampleNode = getExampleNode(example);

            SchemaRegistry apiContext = SchemaValidationContext.createRegistry(schemaDefinition);
            return buildViolationString(validateSchema(exampleNode, apiContext, schemaDefinition));
        } catch (SchemaException ex) {
            throw new RuntimeException(exampleDefinition.getOpenApiFile().getName() + "#" + exampleDefinition.getJsonPointer().toPrettyString() + ": Unable to validate example", ex);
        }
    }

    public static Set<Map.Entry<String, String>> getEnumViolations(SchemaDefinition schemaDefinition) {
        Set<Map.Entry<String, String>> violations = new HashSet<>();
        try {
            SchemaRegistry apiContext = SchemaValidationContext.createRegistry(schemaDefinition);
            Set<JsonNode> enumNodes = getEnumerationNodes(schemaDefinition);
            for (JsonNode nodeToValidate : enumNodes) {
                Optional<String> violation = buildViolationString(validateSchema(nodeToValidate, apiContext, schemaDefinition.getHighLevelSchema()));
                violation.ifPresent(s -> violations.add(Map.entry(nodeToValidate.toString(), s)));
            }
        } catch (SchemaException ex) {
            throw new RuntimeException(schemaDefinition.getOpenApiFile().getName() + "#" + schemaDefinition.getJsonPointer().toPrettyString() + ": Unable to validate enums", ex);
        }
        return violations;
    }

    public static Optional<String> getDefaultValueViolations(SchemaDefinition schemaDefinition) {
        try {
            JsonNode schemaNode = schemaDefinition.getJsonNode();
            if (schemaNode.has("default")) {
                JsonNode defaultNode = schemaNode.get("default");

                var apiContext = SchemaValidationContext.createRegistry(schemaDefinition);
                return buildViolationString(validateSchema(defaultNode, apiContext, schemaDefinition));
            }
        } catch (SchemaException e) {
            throw new RuntimeException(e);
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

    /**
     * @param exampleNode JsonNode object - contains the 'value' of the example object.
     * @return Set<String> with all violations in exampleNode
     */
    private static Set<String> validateSchema(JsonNode exampleNode, SchemaRegistry apiContext, SchemaDefinition schemaDefinition) {
        var validator = apiContext.getSchema(SchemaValidationContext.location(schemaDefinition));
        return validator.validate(exampleNode).stream()
                .map(error -> {
                    String dataCrumbs = dataCrumbs(error.getInstanceLocation());
                    return (dataCrumbs.isEmpty() ? "" : dataCrumbs + ": ") + error.getMessage() +
                            " In Schema: " + schemaDefinition.getOpenApiFile().getName() + "#" + schemaDefinition.getJsonPointer() +
                            " : " + schemaCrumbs(error.getEvaluationPath());
                })
                .collect(Collectors.toSet());
    }

    private static String dataCrumbs(NodePath path) {
        StringBuilder crumbs = new StringBuilder();
        for (int i = 0; i < path.getNameCount(); i++) {
            Object element = path.getElement(i);
            if (element instanceof Integer) {
                crumbs.append("[").append(element).append("]");
            } else {
                if (!crumbs.isEmpty()) {
                    crumbs.append(".");
                }
                crumbs.append(element);
            }
        }
        return crumbs.toString();
    }

    private static String schemaCrumbs(NodePath path) {
        StringBuilder crumbs = new StringBuilder();
        for (int i = 0; i < path.getNameCount(); i++) {
            String name = path.getName(i);
            if ("properties".equals(name)) {
                i++;
            } else if (!(path.getElement(i) instanceof Integer)) {
                if (!crumbs.isEmpty()) {
                    crumbs.append(".");
                }
                crumbs.append("<").append(name).append(">");
            }
        }
        return crumbs.toString();
    }

    private static Optional<SchemaDefinition> getSchemaDefinition(ExampleDefinition exampleDefinition) {
        OpenApiDefinition<?> parentDef = exampleDefinition.getParent();
        Schema schema;
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
        StringBuilder sb = new StringBuilder();
        for (String violation : violations) {
            sb.append(violation).append("\n");
        }
        return Optional.of(sb.toString().strip());
    }
}
