package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class ApiFunctions {

    /**
     * avoid instance creation.
     */
    private ApiFunctions() {
    }

    public static Predicate<SchemaDefinition> getCollectionResponseCondition(Parser.ParserResult parserResult) {
        return (schemaDefinition) -> schemaDefinition.getModel().getProperties() != null &&
                schemaDefinition.getModel().getProperties().containsKey("items") &&
                schemaDefinition.getModel().getProperties().get("items").getType() != null &&
                schemaDefinition.getModel().getProperties().get("items").getType().equals(Schema.SchemaType.ARRAY) &&
                schemaDefinition.getModel().getProperties().get("items").getItems() != null
                && ApiFunctions.isSchemaOfType(schemaDefinition.getModel().getProperties().get("items").getItems(), Schema.SchemaType.OBJECT, parserResult);
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

    private static Set<SchemaDefinition> getSubSchemas(SchemaDefinition schemaDefinition, Parser.ParserResult result, boolean includeTopLevelSchemas) {
        Set<SchemaDefinition> subSchemas = new HashSet<>();
        Predicate<SchemaDefinition> filterSchemaDefinitions = (schemaDef) -> includeTopLevelSchemas || schemaDef.getDefinitionType().equals(OpenApiDefinition.DefinitionType.INLINE);
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

    private static Set<SchemaDefinition> getRecursiveSubSchemas(SchemaDefinition schemaDefinition, Parser.ParserResult result, boolean includeTopLevelSchemas) {
        Set<SchemaDefinition> subSchemas = getSubSchemas(schemaDefinition, result, includeTopLevelSchemas);
        Set<SchemaDefinition> output = new HashSet<>();
        for (SchemaDefinition schema : subSchemas) {
            output.addAll(getRecursiveSubSchemas(schema, result, includeTopLevelSchemas));
        }
        output.addAll(subSchemas);
        return output;
    }

    /**
     * Returns all the properties (schemas) in a complex schema
     *
     * @param result Result from validationparser
     * @param schema Schema of which the properties have to be returned
     */
    public static Map<String, Schema> getRecursiveProperties(Schema schema, Parser.ParserResult result) {
        Map<String, Schema> properties = new HashMap<>();
        SchemaDefinition schemaDef = recursiveResolve(schema, result);
        if (schemaDef.getModel().getProperties() != null) {
            properties.putAll(schemaDef.getModel().getProperties());
        }
        getRecursiveSubSchemas(schemaDef, result, true).forEach(schemaDefinition -> {
            if (schemaDefinition.getModel().getProperties() != null) {
                properties.putAll(schemaDefinition.getModel().getProperties());
            }
        });
        return properties;
    }

    public static Set<String> getRequiredValues(SchemaDefinition schemaDefinition, Parser.ParserResult result) {
        Set<String> requiredValues = new HashSet<>();
        if (schemaDefinition.getModel() != null && schemaDefinition.getModel().getRequired() != null) {
            requiredValues.addAll(schemaDefinition.getModel().getRequired());
        }
        getRecursiveSubSchemas(schemaDefinition, result, false).forEach(schema -> requiredValues.addAll(getRequiredValues(schema, result)));
        return requiredValues;
    }

    public static boolean isPropertyRequiredAndReadOnly(SchemaDefinition schemaDefinition, String propertyName, Parser.ParserResult result) {
        if (schemaDefinition == null || propertyName == null) {
            return false;
        }

        Predicate<SchemaDefinition> propertyRequired = (schemaDef) -> schemaDef.getModel().getRequired() != null && schemaDef.getModel().getRequired().contains(propertyName);
        Predicate<SchemaDefinition> propertyReadOnly = (schemaDef) -> schemaDef.getModel().getProperties() != null && schemaDef.getModel().getProperties().containsKey(propertyName) &&
                schemaDef.getModel().getProperties().get(propertyName).getReadOnly() != null && schemaDef.getModel().getProperties().get(propertyName).getReadOnly();

        Predicate<SchemaDefinition> propertyRequiredAndReadOnly = (schemaDef) -> ((schemaMeetsCondition(schemaDef.getModel(), result, propertyReadOnly) && schemaCanMeetCondition(schemaDef.getModel(), result, propertyRequired)) ||
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
        Predicate<SchemaDefinition> condition = (schemaDefinition) -> schemaDefinition.getModel().getType() == schemaType;
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
        List<org.springframework.http.MediaType> mediaTypeList = new ArrayList<>();
        for (String contentType : contentTypes) {
            mediaTypeList.add(org.springframework.http.MediaType.parseMediaType(contentType));
        }
        return isMediaTypeIncluded(mediaTypeStr, mediaTypeList);
    }

    public static boolean isMediaTypeIncluded(String mediaTypeStr, List<org.springframework.http.MediaType> allowedMediaTypes) {
        org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.parseMediaType(mediaTypeStr);
        for (org.springframework.http.MediaType allowedMediaType : allowedMediaTypes) {
            if (allowedMediaType.includes(mediaType) || (mediaType.getSubtypeSuffix() != null && allowedMediaType.includes(org.springframework.http.MediaType.parseMediaType(mediaType.getType() + "/" + mediaType.getSubtypeSuffix())))) {
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
            if (!(object instanceof String)) {
                continue;
            }
            String string = (String) object;
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