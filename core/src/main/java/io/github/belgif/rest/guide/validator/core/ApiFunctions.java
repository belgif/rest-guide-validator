package io.github.belgif.rest.guide.validator.core;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.model.helper.MediaType;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class ApiFunctions {

    private static final String ITEMS_KEY = "items";

    /**
     * avoid instance creation.
     */
    private ApiFunctions() {
    }

    public static Predicate<SchemaDefinition> getCollectionResponseCondition(Parser.ParserResult parserResult) {
        return schemaDefinition -> schemaDefinition.getModel().getProperties() != null &&
                schemaDefinition.getModel().getProperties().containsKey(ITEMS_KEY) &&
                schemaDefinition.getModel().getProperties().get(ITEMS_KEY).getType() != null &&
                schemaDefinition.getModel().getProperties().get(ITEMS_KEY).getType().equals(Schema.SchemaType.ARRAY) &&
                schemaDefinition.getModel().getProperties().get(ITEMS_KEY).getItems() != null
                && ApiFunctions.isSchemaOfType(schemaDefinition.getModel().getProperties().get(ITEMS_KEY).getItems(), Schema.SchemaType.OBJECT, parserResult);
    }

    public static boolean existsPathWithPathParamAfter(String pathString, Parser.ParserResult result) {
        Set<String> paths = result.getPathDefinitions().stream().map(OpenApiDefinition::getIdentifier).filter(identifier -> identifier.startsWith(pathString)).collect(Collectors.toSet());
        if (paths.isEmpty()) {
            return false;
        } else {
            for (String path : paths) {
                String strippedString = path.substring(pathString.length());
                if (strippedString.startsWith("/{")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verify if schema meets the predicate. anyOf and oneOf schemas all have to match
     *
     * @param result Result from validationparser
     * @param schema Schema that has to be validated
     */
    public static boolean schemaMeetsCondition(Schema schema, Parser.ParserResult result, Predicate<SchemaDefinition> condition) {
        SchemaDefinition schemaDefinition = recursiveResolve(schema, result);
        if (condition.test(schemaDefinition)) {
            return true;
        }
        if (schemaDefinition.getModel().getOneOf() != null && !schemaDefinition.getModel().getOneOf().isEmpty() &&
                schemaDefinition.getModel().getOneOf().stream().allMatch(oneOfSchema -> schemaMeetsCondition(oneOfSchema, result, condition))) {
            return true;
        }
        if (schemaDefinition.getModel().getAnyOf() != null && !schemaDefinition.getModel().getAnyOf().isEmpty() &&
                schemaDefinition.getModel().getAnyOf().stream().allMatch(anyOfSchema -> schemaMeetsCondition(anyOfSchema, result, condition))) {
            return true;
        }
        if (schemaDefinition.getModel().getAllOf() != null && !schemaDefinition.getModel().getAllOf().isEmpty()) {
            return schemaDefinition.getModel().getAllOf().stream().anyMatch(allOfSchema -> schemaMeetsCondition(allOfSchema, result, condition));
        }
        return false;
    }

    /**
     * Verify if schema could possibly meet the predicate. anyOf and oneOf schemas can match
     *
     * @param result Result from validationparser
     * @param schema Schema that has to be validated
     */
    private static boolean schemaCanMeetCondition(Schema schema, Parser.ParserResult result, Predicate<SchemaDefinition> condition) {
        SchemaDefinition schemaDefinition = recursiveResolve(schema, result);
        if (condition.test(schemaDefinition)) {
            return true;
        }
        if (schemaDefinition.getModel().getOneOf() != null && !schemaDefinition.getModel().getOneOf().isEmpty() &&
                schemaDefinition.getModel().getOneOf().stream().anyMatch(oneOfSchema -> schemaCanMeetCondition(oneOfSchema, result, condition))) {
            return true;
        }
        if (schemaDefinition.getModel().getAnyOf() != null && !schemaDefinition.getModel().getAnyOf().isEmpty() &&
                schemaDefinition.getModel().getAnyOf().stream().anyMatch(anyOfSchema -> schemaCanMeetCondition(anyOfSchema, result, condition))) {
            return true;
        }
        if (schemaDefinition.getModel().getAllOf() != null && !schemaDefinition.getModel().getAllOf().isEmpty()) {
            return schemaDefinition.getModel().getAllOf().stream().anyMatch(allOfSchema -> schemaCanMeetCondition(allOfSchema, result, condition));
        }
        return false;
    }

    public static Set<SchemaDefinition> getSubSchemas(SchemaDefinition schemaDefinition, Parser.ParserResult result, boolean includeTopLevelSchemas) {
        Set<SchemaDefinition> subSchemas = new HashSet<>();
        Predicate<SchemaDefinition> filterSchemaDefinitions = schemaDef -> includeTopLevelSchemas || schemaDef.getDefinitionType().equals(OpenApiDefinition.DefinitionType.INLINE);
        if (schemaDefinition.getModel().getAllOf() != null) {
            subSchemas.addAll(schemaDefinition.getModel().getAllOf().stream().map(schema -> recursiveResolve(schema, result)).filter(filterSchemaDefinitions).collect(Collectors.toSet()));
        }
        if (schemaDefinition.getModel().getOneOf() != null) {
            subSchemas.addAll(schemaDefinition.getModel().getOneOf().stream().map(schema -> recursiveResolve(schema, result)).filter(filterSchemaDefinitions).collect(Collectors.toSet()));
        }
        if (schemaDefinition.getModel().getAnyOf() != null) {
            subSchemas.addAll(schemaDefinition.getModel().getAnyOf().stream().map(schema -> recursiveResolve(schema, result)).filter(filterSchemaDefinitions).collect(Collectors.toSet()));
        }
        return subSchemas;
    }

    /**
     * @return all subschemas of given schema, including recursively referenced ones
     */
    private static Set<SchemaDefinition> getAllSubSchemas(SchemaDefinition schemaDefinition, Parser.ParserResult result, boolean includeTopLevelSchemas) {
        Set<SchemaDefinition> subSchemas = getSubSchemas(schemaDefinition, result, includeTopLevelSchemas);
        Set<SchemaDefinition> output = new HashSet<>();
        for (SchemaDefinition schema : subSchemas) {
            output.addAll(getAllSubSchemas(schema, result, includeTopLevelSchemas));
        }
        output.addAll(subSchemas);
        return output;
    }

    /**
     * Returns all the properties (with their schemas) at top-level of a possibly composite schema.
     * Properties only reachable via discriminator are not added, unless valueNode parameter is used.
     *
     * @param result      Result from validationparser
     * @param schema      Schema of which the properties have to be returned
     * @param valueNode   optional JSON value compliant with the schema
     */
    public static Map<String, Schema> getAllProperties(Schema schema, Parser.ParserResult result, JsonNode valueNode) {
        Map<String, Schema> properties = new HashMap<>();
        SchemaDefinition schemaDef = recursiveResolve(schema, result);
        Set<SchemaDefinition> definitions = new HashSet<>();
        definitions.add(schemaDef);
        definitions.addAll(getAllSubSchemas(schemaDef, result, true));
        if (valueNode != null) {
            definitions.addAll(findDiscriminatorSchemas(definitions, result, valueNode));
        }
        definitions.forEach(schemaDefinition -> {
            if (schemaDefinition.getModel().getProperties() != null) {
                properties.putAll(schemaDefinition.getModel().getProperties());
            }
        });
        return properties;
    }

    private static Set<SchemaDefinition> findDiscriminatorSchemas(Set<SchemaDefinition> schemaDefinitions, Parser.ParserResult result, JsonNode valueNode) {
        Set<SchemaDefinition> schemaDefinitionsWithDiscriminators = schemaDefinitions.stream()
                .filter(def -> def.getModel().getDiscriminator() != null && valueNode.has(def.getModel().getDiscriminator().getPropertyName()))
                .collect(Collectors.toSet());

        Set<SchemaDefinition> schemaDefinitionMappings = new HashSet<>();
        for (SchemaDefinition schemaDefinition : schemaDefinitionsWithDiscriminators) {
            String discriminator = schemaDefinition.getModel().getDiscriminator().getPropertyName();
            String discriminatorValue = valueNode.get(discriminator).asText();
            String mapping;
            if (schemaDefinition.getModel().getDiscriminator().getMapping() != null) {
                mapping = schemaDefinition.getModel().getDiscriminator().getMapping().get(discriminatorValue);
            } else {
                // TODO: if mapping present, but property value not found in discriminator mapping, also fallback to this
                mapping = "#/components/schemas/" + discriminatorValue;
            }

            try {
                OpenApiDefinition<Schema> resolvedDef = result.resolve(mapping, schemaDefinition.getOpenApiFile());
                SchemaDefinition schemaDef = (SchemaDefinition) resolvedDef;
                schemaDefinitionMappings.add(schemaDef);
                schemaDefinitionMappings.addAll(ApiFunctions.getAllSubSchemas(schemaDef, result, true)); // TODO: move this to caller?
            } catch (RuntimeException e) {
                log.error("Cannot find schema for discriminator {} : {}", discriminator, discriminatorValue);
            }
        }
        return schemaDefinitionMappings;
    }

    public static Set<String> getRequiredProperties(SchemaDefinition schemaDefinition, Parser.ParserResult result) {
        Set<String> requiredProperties = new HashSet<>();
        if (schemaDefinition.getModel() != null && schemaDefinition.getModel().getRequired() != null) {
            requiredProperties.addAll(schemaDefinition.getModel().getRequired());
        }
        getAllSubSchemas(schemaDefinition, result, false).forEach(schema -> requiredProperties.addAll(getRequiredProperties(schema, result)));
        return requiredProperties;
    }

    public static boolean isPropertyRequiredAndReadOnly(SchemaDefinition schemaDefinition, String propertyName, Parser.ParserResult result) {
        if (schemaDefinition == null || propertyName == null) {
            return false;
        }

        Predicate<SchemaDefinition> propertyRequired = schemaDef -> schemaDef.getModel().getRequired() != null && schemaDef.getModel().getRequired().contains(propertyName);
        Predicate<SchemaDefinition> propertyReadOnly = schemaDef -> schemaDef.getModel().getProperties() != null && schemaDef.getModel().getProperties().containsKey(propertyName) &&
                schemaDef.getModel().getProperties().get(propertyName).getReadOnly() != null && schemaDef.getModel().getProperties().get(propertyName).getReadOnly();

        Predicate<SchemaDefinition> propertyRequiredAndReadOnly = schemaDef -> ((schemaMeetsCondition(schemaDef.getModel(), result, propertyReadOnly) && schemaCanMeetCondition(schemaDef.getModel(), result, propertyRequired)) ||
                (schemaCanMeetCondition(schemaDef.getModel(), result, propertyReadOnly) && schemaMeetsCondition(schemaDef.getModel(), result, propertyRequired)));

        return propertyRequiredAndReadOnly.test(schemaDefinition) &&
                getSubSchemas(schemaDefinition, result, true).stream().noneMatch(propertyRequiredAndReadOnly);
    }

    /**
     * Returns true if schematype or any indirectly referenced type (allOf, anyOf, oneOf) is schemaType.
     * Returns false if schema allows more types than the specified one.
     *
     * @param result     Result from validationparser
     * @param schema     Schema that has to be validated
     * @param schemaType type of the schema wanted, eg. object, string, array etc.
     */
    public static boolean isSchemaOfType(Schema schema, Schema.SchemaType schemaType, Parser.ParserResult result) {
        Predicate<SchemaDefinition> condition = schemaDefinition -> schemaDefinition.getModel().getType() == schemaType;
        return schemaMeetsCondition(schema, result, condition);
    }

    public static SchemaDefinition recursiveResolve(Schema schema, Parser.ParserResult result) {
        SchemaDefinition resolvedSchema = (SchemaDefinition) result.resolve(schema);
        if (resolvedSchema.getModel().getRef() == null) {
            return resolvedSchema;
        } else {
            return recursiveResolve(resolvedSchema.getModel(), result);
        }
    }

    public static boolean isMediaTypeIncluded(String mediaTypeStr, Set<String> contentTypes) {
        List<MediaType> mediaTypeList = new ArrayList<>();
        for (String contentType : contentTypes) {
            mediaTypeList.add(new MediaType(contentType));
        }
        return isMediaTypeIncluded(mediaTypeStr, mediaTypeList);
    }

    public static boolean isMediaTypeIncluded(String mediaTypeStr, List<MediaType> allowedMediaTypes) {
        MediaType mediaType = new MediaType(mediaTypeStr);
        for (MediaType allowedMediaType : allowedMediaTypes) {
            if (allowedMediaType.includes(mediaType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLowerCamelCase(List<Object> objects) {
        return isLowerCamelCase(objects, null);
    }

    public static boolean isLowerCamelCase(List<Object> objects, String stripCharacter) {
        for (Object object : objects) {
            if (!(object instanceof String string)) {
                continue;
            }
            if (stripCharacter != null && !stripCharacter.isEmpty()) {
                string = string.replaceAll(stripCharacter, "");
            }
            if (!isLowerCamelCase(string)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isLowerCamelCase(String string) {
        return isLowerCamelCase(string, null);
    }

    public static boolean isLowerCamelCase(String string, List<String> extraCharacters) {
        String regexStart = "^[a-z0-9]+([A-Z";
        String regexEnd = "]?[a-z0-9]+)*[a-zA-Z0-9]?$";
        StringBuilder sb = new StringBuilder();
        sb.append(regexStart);
        if (extraCharacters != null) {
            for (String character : extraCharacters) {
                sb.append("\\").append(character);
            }
        }
        sb.append(regexEnd);
        return string.matches(sb.toString());
    }

    public static boolean isUpperCamelCase(String string) {
        return string.matches("^(?!.*[-_.])([A-Z][a-z0-9]+)*$");
    }

    public static boolean isUpperKebabCase(String string) {
        return string.matches("^[A-Z0-9]([a-zA-Z0-9](-[A-Z0-9])?)*$");
    }

    public static boolean isNotInSet(String string, Set<String> set) {
        if (string == null || set == null) {
            return true;
        }
        return !set.contains(string);
    }

}