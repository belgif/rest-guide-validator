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
        try {
            var responses = path.getModel().getGET().getResponses().getAPIResponses().values().stream().flatMap(apiResponse -> apiResponse.getContent().getMediaTypes().values().stream()).map(MediaType::getSchema);
            responses.forEach(inlineSchema -> {
                    SchemaDefinition schemaDefinition = (SchemaDefinition) result.resolve(inlineSchema);
                    Schema schema = schemaDefinition.getModel();
                    if (schema.getProperties() != null && schema.getProperties().containsKey("items") && schema.getProperties().get("items").getType() != null && schema.getProperties().get("items").getType().equals(Schema.SchemaType.ARRAY)) {
                        isCollection.set(true);
                    }
            });
        } catch (NullPointerException ignored) {}
        return isCollection.get();
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
        if (resolvedSchema.getModel().getType() == schemaType) {
            return true;
        }
        if (resolvedSchema.getModel().getOneOf() != null && !resolvedSchema.getModel().getOneOf().isEmpty()) {
            return resolvedSchema.getModel().getOneOf().stream().allMatch(oneOfSchema -> isSchemaOfType(oneOfSchema, schemaType, result));
        }
        if (resolvedSchema.getModel().getAnyOf() != null && !resolvedSchema.getModel().getAnyOf().isEmpty()) {
            return resolvedSchema.getModel().getAnyOf().stream().allMatch(anyOfSchema -> isSchemaOfType(anyOfSchema, schemaType, result));
        }
        if (resolvedSchema.getModel().getAllOf() != null && !resolvedSchema.getModel().getAllOf().isEmpty()) {
            return resolvedSchema.getModel().getAllOf().stream().anyMatch(allOfSchema -> isSchemaOfType(allOfSchema, schemaType, result));
        }
        return false;
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
        for (Object object : objects) {
            String string = (String) object;
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

    public static boolean isIncluded(String string, Set<String> set) {
        if (string == null || set == null) {
            return true;
        }
        return !set.contains(string);
    }

}