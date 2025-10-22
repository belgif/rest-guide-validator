package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.belgif.rest.guide.validator.core.model.*;
import io.github.belgif.rest.guide.validator.core.model.helper.BelgifOAI3Context;
import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.SourceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.OAIContext;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.ValidationData;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class SchemaValidator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
    // Map of globally resolved nodes for caching.
    private static final Map<File, JsonNode> openApiNodesCache = new ConcurrentHashMap<>();
    private static final Map<OpenApiDefinition<?>, JsonNode> schemaNodesCache = new ConcurrentHashMap<>();

    private SchemaValidator() {
    }

    public static Optional<String> getExampleViolations(ExampleDefinition exampleDefinition) {
        try {
            SchemaDefinition schemaDefinition = getSchemaDefinition(exampleDefinition);
            JsonNode schemaNode = getSchemaNode(schemaDefinition);
            ExampleDefinition example = (ExampleDefinition) exampleDefinition.getResult().resolve(exampleDefinition.getModel());
            JsonNode exampleNode = getExampleNode(example);

            OAIContext apiContext = new BelgifOAI3Context(schemaDefinition);
            return buildViolationString(validateSchema(schemaNode, exampleNode, apiContext, schemaDefinition));
        } catch (ResolutionException ex) {
            throw new RuntimeException(exampleDefinition.getOpenApiFile().getName() + "#" + exampleDefinition.getJsonPointer().toPrettyString() + ": Unable to validate example", ex);
        }
    }

    public static Set<Map.Entry<String, String>> getEnumViolations(SchemaDefinition schemaDefinition) {
        Set<Map.Entry<String, String>> violations = new HashSet<>();
        JsonNode schemaNode = getSchemaNode(schemaDefinition.getHighLevelSchema());
        try {
            OAIContext apiContext = new BelgifOAI3Context(schemaDefinition);
            Set<JsonNode> enumNodes = getEnumerationNodes(schemaDefinition);
            for (JsonNode nodeToValidate : enumNodes) {
                Optional<String> violation = buildViolationString(validateSchema(schemaNode, nodeToValidate, apiContext, schemaDefinition.getHighLevelSchema()));
                violation.ifPresent(s -> violations.add(Map.entry(nodeToValidate.toString(), s)));
            }
        } catch (ResolutionException ex) {
            throw new RuntimeException(schemaDefinition.getOpenApiFile().getName() + "#" + schemaDefinition.getJsonPointer().toPrettyString() + ": Unable to validate enums", ex);
        }
        return violations;
    }

    public static Optional<String> getDefaultValueViolations(SchemaDefinition schemaDefinition) {
        try {
            JsonNode schemaNode = getSchemaNode(schemaDefinition);
            if (schemaNode.has("default")) {
                JsonNode defaultNode = schemaNode.get("default");

                var apiContext = new OAI3Context(new URL(schemaDefinition.getOpenApiFile().toURI().toString()));
                return buildViolationString(validateSchema(schemaNode, defaultNode, apiContext, schemaDefinition));
            }
        } catch (MalformedURLException | ResolutionException e) {
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
        JsonNode openApiNode = getOpenApiNode(exampleDefinition);
        JsonNode exampleNode = getNodeAtLocation(openApiNode, exampleDefinition.getJsonPointer());
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
        JsonNode openApiNode = getOpenApiNode(schemaDefinition);
        JsonNode schema = getNodeAtLocation(openApiNode, schemaDefinition.getJsonPointer());
        for (JsonNode enumeration : schema.get("enum")) {
            enumerations.add(enumeration);
        }
        return enumerations;
    }

    /**
     * @param schemaNode  JsonNode object - contains $ref to schema to validate against as first property. Rest of the node is a copy of the merged openapi
     * @param exampleNode JsonNode object - contains the 'value' of the example object.
     * @return Set<String> with all violations in exampleNode
     */
    private static Set<String> validateSchema(JsonNode schemaNode, JsonNode exampleNode, OAIContext apiContext, SchemaDefinition schemaDefinition) {
        var validationContext = new ValidationContext<OAI3>(apiContext);
        org.openapi4j.schema.validator.v3.SchemaValidator validator = new org.openapi4j.schema.validator.v3.SchemaValidator(validationContext, null, schemaNode);

        ValidationData<Void> validation = new ValidationData<>();
        validator.validate(exampleNode, validation);
        if (!validation.isValid()) {
            return validation.results().items().stream()
                    .map(validationItem ->
                            (!validationItem.dataCrumbs().isEmpty() ? validationItem.dataCrumbs() + ": " : "") + validationItem.message() + " " +
                                    "In Schema: " + schemaDefinition.getOpenApiFile().getName() + "#" + schemaDefinition.getJsonPointer() +
                                    " : " + (validationItem.dataCrumbs().isEmpty() ? validationItem.schemaCrumbs() : validationItem.schemaCrumbs().replaceFirst(validationItem.dataCrumbs() + ".", ""))
                    )
                    .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    /**
     * @param parent - Definition object of the example parent. Schema to validate against is derived from this
     * @return JsonNode object of the schema that has to be validated against.
     */
    public static JsonNode getSchemaNode(OpenApiDefinition<?> parent) {
        return schemaNodesCache.computeIfAbsent(parent, parentDef -> {
            JsonNode openApiNode = getOpenApiNode(parentDef);
            return getNodeAtLocation(openApiNode, parentDef.getJsonPointer());
        });
    }

    private static SchemaDefinition getSchemaDefinition(ExampleDefinition exampleDefinition) {
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
        return (SchemaDefinition) exampleDefinition.getResult().resolve(schema);
    }

    private static JsonNode getOpenApiNode(OpenApiDefinition<?> parent) {
        return openApiNodesCache.computeIfAbsent(parent.getOpenApiFile(), parentFile -> {
            SourceDefinition sourceDefinition = parent.getResult().getSrc().get(parent.getOpenApiFile().getAbsolutePath());
            try {
                return sourceDefinition.isYaml() ? yamlReader.readTree(parentFile) : mapper.readTree(parentFile);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read openapi file <<" + parentFile.getAbsolutePath() + ">> for example validation", e);
            }
        });
    }

    private static JsonNode getNodeAtLocation(JsonNode openApiNode, JsonPointer jsonPointer) {
        return openApiNode.at(com.fasterxml.jackson.core.JsonPointer.compile(jsonPointer.getJsonPointer()));
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
