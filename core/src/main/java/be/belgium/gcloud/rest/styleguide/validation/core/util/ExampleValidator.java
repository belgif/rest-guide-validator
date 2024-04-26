package be.belgium.gcloud.rest.styleguide.validation.core.util;

import be.belgium.gcloud.rest.styleguide.validation.core.model.ExampleDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.model.OpenApiDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationResult;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ExampleValidator {

    private static final ObjectMapper mapper = new ObjectMapper();
    // Map of globally resolved nodes for caching.
    private static final Map<File, JsonNode> openApiNodesCache = new ConcurrentHashMap<>();
    private static final Map<OpenApiDefinition<?>, JsonNode> schemaNodesCache = new ConcurrentHashMap<>();

    public static Set<String> getExampleViolations(ExampleDefinition exampleDefinition) {
        try {
            JsonNode schemaNode = getSchemaNode(exampleDefinition.getParent());
            ExampleDefinition example = (ExampleDefinition) exampleDefinition.getResult().resolve(exampleDefinition.getModel());
            JsonNode exampleNode = mapper.valueToTree(example.getModel().getValue());

            return validateExample(schemaNode, exampleNode);
        } catch (ResolutionException ex) {
            throw new RuntimeException(exampleDefinition.getOpenApiFile().getName() + "#" + exampleDefinition.getJsonPointer().toPrettyString() + ": Unable to validate example", ex);
        }
    }

    // Internal methods

    /**
     * @param schemaNode  JsonNode object - contains $ref to schema to validate against as first property. Rest of the node is a copy of the merged openapi
     * @param exampleNode JsonNode object - contains the 'value' of the example object.
     * @return Set<String> with all violations in exampleNode
     * @throws ResolutionException thrown if SchemaValidator object cannot be instantiated (i.e. unable to resolve references)
     */
    private static Set<String> validateExample(JsonNode schemaNode, JsonNode exampleNode) throws ResolutionException {
        SchemaValidator validator = new SchemaValidator(null, schemaNode);

        ValidationData<Void> validation = new ValidationData<>();
        validator.validate(exampleNode, validation);
        if (!validation.isValid()) {
            return validation.results().items().stream().map(ValidationResult::message).collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    /**
     * @param parent - Definition object of the example parent. Schema to validate against is derived from this
     * @return JsonNode object starting with property $ref with value of the relative JsonPointer of the schema to validate against.
     * After $ref the merged openapi nodes (no external references) of the parent.
     * This is a workaround to let the OpenApi4j validator work. It starts reading the schema from the root node, and the context to other references is still necessary
     */
    private static JsonNode getSchemaNode(OpenApiDefinition<?> parent) {
        return schemaNodesCache.computeIfAbsent(parent, parentDef -> {
            JsonNode openApiNode = getOpenApiNode(parentDef);
            JsonNode node = getNodeAtLocation(openApiNode, parentDef.getJsonPointer());
            String pointer;
            if (node.has("schema")) {
                pointer = "#" + parentDef.getJsonPointer().getJsonPointer() + "/schema";
            } else {
                pointer = "#" + parentDef.getJsonPointer().getJsonPointer();
            }
            ObjectNode mergedNode = mapper.createObjectNode();
            mergedNode.put("$ref", pointer);
            mergedNode.setAll((ObjectNode) openApiNode);
            return mergedNode;
        });
    }

    private static JsonNode getOpenApiNode(OpenApiDefinition<?> parent) {
        return openApiNodesCache.computeIfAbsent(parent.getOpenApiFile(), parentFile -> {
            var openApiParser = new OpenAPIParser();
            var parseOptions = new ParseOptions();
            parseOptions.setResolve(true);

            var parserResult = openApiParser.readLocation(parentFile.getAbsolutePath(), null, parseOptions);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            return mapper.valueToTree(parserResult.getOpenAPI());
        });
    }


    private static JsonNode getNodeAtLocation(JsonNode openApiNode, JsonPointer jsonPointer) {
        return openApiNode.at(com.fasterxml.jackson.core.JsonPointer.compile(jsonPointer.getJsonPointer()));
    }

}
