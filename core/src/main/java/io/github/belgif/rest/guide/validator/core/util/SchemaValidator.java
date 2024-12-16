package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.belgif.rest.guide.validator.core.ApiFunctions;
import io.github.belgif.rest.guide.validator.core.model.*;
import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.JsonPointerOas2Exception;
import io.github.belgif.rest.guide.validator.core.parser.SourceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.core.model.v3.OAI3Context;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.ValidationData;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
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

    public static List<String> getNonDefinedProperties(ExampleDefinition exampleDefinition) {
        SchemaDefinition schemaDefinition = getSchemaDefinition(exampleDefinition);
        ExampleDefinition example = (ExampleDefinition) exampleDefinition.getResult().resolve(exampleDefinition.getModel());
        JsonNode exampleNode = getExampleNode(example);
        if (schemaDefinition.getModel().getAdditionalPropertiesBoolean() != null && schemaDefinition.getModel().getAdditionalPropertiesBoolean()) {
            return new ArrayList<>();
        }
        return getNonDefinedProperties(exampleNode, schemaDefinition);
    }

    private static List<String> getNonDefinedProperties(JsonNode exampleNode, SchemaDefinition schemaDefinition) {
        List<String> missingProperties = new ArrayList<>();
        Iterator<String> exampleFieldNames = exampleNode.fieldNames();
        Map<String, Schema> definedProperties = ApiFunctions.getRecursiveProperties(schemaDefinition.getModel(), schemaDefinition.getResult());
        while (exampleFieldNames.hasNext()) {
            String exampleFieldName = exampleFieldNames.next();
            if (!definedProperties.containsKey(exampleFieldName)) {
                missingProperties.add(exampleFieldName + " in: #" + schemaDefinition.getPrintableJsonPointer());
            } else {
                SchemaDefinition def = (SchemaDefinition) schemaDefinition.getResult().resolve(definedProperties.get(exampleFieldName));
                //TODO check what to do when schemaType is array
                if (subSchemaShouldBeInspectedFurther(def)) {
                    missingProperties.addAll(getNonDefinedProperties(exampleNode.get(exampleFieldName), def));
                }
            }
        }
        return missingProperties;
    }

//    private static List<String> getNonDefinedProperties(JsonNode exampleNode, SchemaDefinition schemaDefinition) {
//        List<String> missingProperties = new ArrayList<>();
//        Iterator<String> exampleFieldNames = exampleNode.fieldNames();
//        Set<SchemaDefinition> subSchemas = ApiFunctions.getSubSchemas(schemaDefinition, schemaDefinition.getResult(), true);
//        subSchemas.add(schemaDefinition);
//        while (exampleFieldNames.hasNext()) {
//            String exampleFieldName = exampleFieldNames.next();
//            List<SchemaDefinition> subSchemasContainingFieldName = subSchemas.stream()
//                    .filter(schema -> schema.getModel().getProperties() != null && schema.getModel().getProperties().containsKey(exampleFieldName)).toList();
//            if (subSchemasContainingFieldName.isEmpty()) {
//                missingProperties.add(exampleFieldName + " in: #" + schemaDefinition.getPrintableJsonPointer());
//            } else {
//                for (SchemaDefinition subSchema : subSchemasContainingFieldName) {
//                    Schema schema = subSchema.getModel().getProperties().get(exampleFieldName);
//                    SchemaDefinition def = (SchemaDefinition) subSchema.getResult().resolve(schema);
//                    //TODO check what to do when schemaType is array
//                    if (subSchemaShouldBeInspectedFurther(def)) {
//                        missingProperties.addAll(getNonDefinedProperties(exampleNode.get(exampleFieldName), def));
//                    }
//                }
//            }
//        }
//        return missingProperties;
//    }

    private static boolean subSchemaShouldBeInspectedFurther(SchemaDefinition schemaDefinition) {
        Schema schema = schemaDefinition.getModel();
        return (schema.getType() != null && schema.getType().equals(Schema.SchemaType.OBJECT)) ||
                schema.getAllOf() != null || schema.getAnyOf() != null || schema.getOneOf() != null;
    }

    public static String getExampleViolations(ExampleDefinition exampleDefinition) {
        try {
            SchemaDefinition schemaDefinition = getSchemaDefinition(exampleDefinition);
            JsonNode schemaNode = getSchemaNode(schemaDefinition);
            ExampleDefinition example = (ExampleDefinition) exampleDefinition.getResult().resolve(exampleDefinition.getModel());
            JsonNode exampleNode = getExampleNode(example);

            var apiContext = new OAI3Context(new URL(schemaDefinition.getOpenApiFile().toURI().toString()));
            return buildViolationString(validateSchema(schemaNode, exampleNode, apiContext, schemaDefinition));
        } catch (ResolutionException ex) {
            throw new RuntimeException(exampleDefinition.getOpenApiFile().getName() + "#" + exampleDefinition.getSrcVersionedJsonPointer().toPrettyString() + ": Unable to validate example", ex);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (JsonPointerOas2Exception e) {
            log.debug("Unable to validate example: {}", exampleDefinition.getJsonPointer().toPrettyString());
            return null;
        } catch (Exception e) {
            if (exampleDefinition.getResult().getOasVersion() == 2) {
                /*
                It seems impossible to predict all JsonPointer translation mistakes from OAS3 to OAS2.
                To not let builds fail due to shortcomings of the validator, these parsing exceptions are ignored for OAS2 contracts.
                 */
                log.warn("Unable to validate example due to OAS2 incompatibility: {}", exampleDefinition.getJsonPointer().toPrettyString());
                return null;
            } else {
                throw e;
            }
        }
    }

    public static Set<Map.Entry<String, String>> getEnumViolations(SchemaDefinition schemaDefinition) {
        Set<Map.Entry<String, String>> violations = new HashSet<>();
        try {
            JsonNode schemaNode = getSchemaNode(schemaDefinition.getHighLevelSchema());
            try {
                var apiContext = new OAI3Context(new URL(schemaDefinition.getOpenApiFile().toURI().toString()));
                Set<JsonNode> enumNodes = getEnumerationNodes(schemaDefinition);
                for (JsonNode nodeToValidate : enumNodes) {
                    String violation = buildViolationString(validateSchema(schemaNode, nodeToValidate, apiContext, schemaDefinition.getHighLevelSchema()));
                    if (violation != null) {
                        violations.add(Map.entry(nodeToValidate.toString(), violation));
                    }
                }
            } catch (ResolutionException ex) {
                throw new RuntimeException(schemaDefinition.getOpenApiFile().getName() + "#" + schemaDefinition.getSrcVersionedJsonPointer().toPrettyString() + ": Unable to validate enums", ex);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } catch (JsonPointerOas2Exception e) {
            log.debug("Unable to validate enums in schema definition: {}", schemaDefinition.getHighLevelSchema().getJsonPointer().toPrettyString());
        } catch (Exception e) {
            if (schemaDefinition.getResult().getOasVersion() == 2) {
                /*
                It seems impossible to predict all JsonPointer translation mistakes from OAS3 to OAS2.
                To not let builds fail due to shortcomings of the validator, these parsing exceptions are ignored for OAS2 contracts.
                 */
                log.warn("Unable to validate enum due to OAS2 incompatibility: {}", schemaDefinition.getJsonPointer().toPrettyString());
            } else {
                throw e;
            }
        }

        return violations;
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
        JsonNode exampleNode = getNodeAtLocation(openApiNode, exampleDefinition.getSrcVersionedJsonPointer());
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
        JsonNode schema = getNodeAtLocation(openApiNode, schemaDefinition.getSrcVersionedJsonPointer());
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
    private static Set<String> validateSchema(JsonNode schemaNode, JsonNode exampleNode, OAI3Context apiContext, SchemaDefinition schemaDefinition) {
        var validationContext = new ValidationContext<OAI3>(apiContext);
        org.openapi4j.schema.validator.v3.SchemaValidator validator = new org.openapi4j.schema.validator.v3.SchemaValidator(validationContext, null, schemaNode);

        ValidationData<Void> validation = new ValidationData<>();
        validator.validate(exampleNode, validation);
        if (!validation.isValid()) {
            return validation.results().items().stream()
                    .map(validationItem ->
                            (!validationItem.dataCrumbs().isEmpty() ? validationItem.dataCrumbs() + ": " : "") + validationItem.message() + " " +
                                    "In Schema: " + schemaDefinition.getOpenApiFile().getName() + "#" + schemaDefinition.getSrcVersionedJsonPointer() +
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
            return getNodeAtLocation(openApiNode, parentDef.getSrcVersionedJsonPointer());
        });
    }

    private static SchemaDefinition getSchemaDefinition(ExampleDefinition exampleDefinition) {
        OpenApiDefinition<?> parentDef = exampleDefinition.getParent();
        Schema schema;
        if (parentDef instanceof SchemaDefinition) {
            schema = ((SchemaDefinition) parentDef).getModel();
        } else if (parentDef instanceof MediaTypeDefinition) {
            schema = ((MediaTypeDefinition) parentDef).getModel().getSchema();
        } else if (parentDef instanceof ParameterDefinition) {
            schema = ((ParameterDefinition) parentDef).getModel().getSchema();
        } else if (parentDef instanceof ResponseHeaderDefinition) {
            schema = ((ResponseHeaderDefinition) parentDef).getModel().getSchema();
        } else {
            throw new RuntimeException("[Internal Error] Unable to find schema related to example: " + exampleDefinition.getSrcVersionedJsonPointer());
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

    private static String buildViolationString(Set<String> violations) {
        if (violations.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String violation : violations) {
            sb.append(violation).append("\n");
        }
        return sb.toString().strip();
    }
}
