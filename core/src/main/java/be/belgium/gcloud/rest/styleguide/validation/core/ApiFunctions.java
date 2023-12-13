package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.core.model.OpenApiDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.model.PathDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.model.SchemaDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class ApiFunctions {

    /**
     * avoid instance creation.
     */
    private ApiFunctions() {
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

    public static boolean hasCollectionResponse(PathDefinition path, Parser.ParserResult result) {
        AtomicBoolean isCollection = new AtomicBoolean(false);
        Predicate<SchemaDefinition> condition = (schemaDefinition) -> schemaDefinition.getModel().getProperties() != null &&
                schemaDefinition.getModel().getProperties().containsKey("items") &&
                schemaDefinition.getModel().getProperties().get("items").getType() != null &&
                schemaDefinition.getModel().getProperties().get("items").getType().equals(Schema.SchemaType.ARRAY)
                && isSchemaOfType(recursiveResolve(schemaDefinition.getModel().getProperties().get("items").getItems(), result).getModel(), Schema.SchemaType.OBJECT, result)
                ;
        try {
            var responses = path.getModel().getGET().getResponses().getAPIResponses().values().stream().flatMap(apiResponse -> apiResponse.getContent().getMediaTypes().values().stream()).map(MediaType::getSchema);
            responses.forEach(inlineSchema -> {
                SchemaDefinition schemaDefinition = (SchemaDefinition) result.resolve(inlineSchema);
                if (schemaMeetsCondition(schemaDefinition, result, condition)) {
                    isCollection.set(true);
                }
            });
        } catch (NullPointerException ignored) {
        }
        return isCollection.get();
    }

    private static boolean schemaMeetsCondition(SchemaDefinition schemaDefinition, Parser.ParserResult result, Predicate<SchemaDefinition> condition) {
        if (condition.test(schemaDefinition)) {
            return true;
        }
        if (schemaDefinition.getModel().getOneOf() != null && !schemaDefinition.getModel().getOneOf().isEmpty()) {
            return schemaDefinition.getModel().getOneOf().stream().allMatch(oneOfSchema -> schemaMeetsCondition(recursiveResolve(oneOfSchema, result), result, condition));
        }
        if (schemaDefinition.getModel().getAnyOf() != null && !schemaDefinition.getModel().getAnyOf().isEmpty()) {
            return schemaDefinition.getModel().getAnyOf().stream().allMatch(anyOfSchema -> schemaMeetsCondition(recursiveResolve(anyOfSchema, result), result, condition));
        }
        if (schemaDefinition.getModel().getAllOf() != null && !schemaDefinition.getModel().getAllOf().isEmpty()) {
            return schemaDefinition.getModel().getAllOf().stream().anyMatch(allOfSchema -> schemaMeetsCondition(recursiveResolve(allOfSchema, result), result, condition));
        }
        return false;
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
        SchemaDefinition resolvedSchema = recursiveResolve(schema, result);
        Predicate<SchemaDefinition> condition = (schemaDefinition) -> schemaDefinition.getModel().getType() == schemaType;
        return schemaMeetsCondition(resolvedSchema, result, condition);
    }

    private static SchemaDefinition recursiveResolve(Schema schema, Parser.ParserResult result) {
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
        String regexEnd = "]?[a-z0-9]+)*$";
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