package be.belgium.gcloud.rest.styleguide.validation.core.parser;

import be.belgium.gcloud.rest.styleguide.validation.LineRangePath;
import be.belgium.gcloud.rest.styleguide.validation.core.ApiFunctions;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.eclipse.microprofile.openapi.models.headers.Header;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.models.*;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class Parser {

    private File openApiFile;

    @Getter
    @Setter
    public static class ParserResult {
        private Set<MediaTypeDefinition> mediaTypes = new HashSet<>();
        private Set<RequestBodyDefinition> requestBodies = new HashSet<>();
        private Set<ResponseDefinition> responses = new HashSet<>();
        private Set<OperationDefinition> operations = new HashSet<>();
        private Set<SchemaDefinition> schemas = new HashSet<>();
        private Set<ParameterDefinition> parameters = new HashSet<>();
        private Set<ResponseHeaderDefinition> headers = new HashSet<>();
        private Set<OpenApiDefinition<? extends Constructible>> allDefinitions = new HashSet<>();

        public String jsonString;
        private OpenAPI openAPI;
        private List<LineRangePath> paths;

        private void assembleAllDefinitions() {
            allDefinitions.addAll(requestBodies);
            allDefinitions.addAll(responses);
            allDefinitions.addAll(operations);
            allDefinitions.addAll(mediaTypes);
            allDefinitions.addAll(schemas);
            allDefinitions.addAll(parameters);
            allDefinitions.addAll(headers);
        }

        public <T extends Constructible> OpenApiDefinition<T> resolve(T model) {
            if (model instanceof Reference) {
                String ref = ((Reference<?>) model).getRef();
                if (ref != null) {
                    String id = getRefName(ref);
                    List<OpenApiDefinition<? extends Constructible>> foundRefMatches = allDefinitions.stream().filter(def -> id.equals(def.getIdentifier())).collect(Collectors.toList());
                    Optional<OpenApiDefinition<? extends Constructible>> defMatch;
                    if (model instanceof Parameter) {
                        defMatch = foundRefMatches.stream().filter(def -> def instanceof ParameterDefinition).findAny();
                    } else if (model instanceof RequestBody) {
                        defMatch = foundRefMatches.stream().filter(def -> def instanceof RequestBodyDefinition).findAny();
                    } else if (model instanceof APIResponse) {
                        defMatch = foundRefMatches.stream().filter(def -> def instanceof ResponseDefinition).findAny();
                    } else if (model instanceof Schema) {
                        defMatch = foundRefMatches.stream().filter(def -> def instanceof SchemaDefinition).findAny();
                    } else if (model instanceof MediaType) {
                        defMatch = foundRefMatches.stream().filter(def -> def instanceof MediaTypeDefinition).findAny();
                    } else {
                        defMatch = foundRefMatches.stream().findAny();
                    }
                    if (defMatch.isPresent()) {
                        return (OpenApiDefinition<T>) defMatch.get();
                    } else {
                        throw new RuntimeException("[Internal error] Could not find match of " + ref);
                    }
                }
            }

            // no ref
            var defMatch = allDefinitions.stream().filter(def -> def.getModel() == model).findAny();
            if (defMatch.isPresent()) {
                return (OpenApiDefinition<T>) defMatch.get();
            } else {
                throw new RuntimeException("[Internal error] Could not find match of " + model.toString());
            }
        }

        private String getRefName(String ref) {
            if (!ref.contains("/")) return ref;
            return ref.substring(ref.lastIndexOf('/') + 1);
        }

    }

    public ParserResult parse(OpenApiViolationAggregator openApiViolationAggregator) {
        try {
            var openAPI = ApiFunctions.buildOpenApiSpecification(openApiFile, openApiViolationAggregator);
            ParserResult result = new ParserResult();
            result.paths = ApiFunctions.buildAllPathWithLineRange(openAPI, openApiViolationAggregator);
            result.openAPI = openAPI;
            result.jsonString = getJsonString(openApiViolationAggregator);
            parseComponents(openAPI.getComponents(), result);
            parsePaths(openAPI.getPaths(), result);
            result.assembleAllDefinitions();
            return result;
        } catch (IOException e) {
            openApiViolationAggregator.addViolation(e.getClass().getSimpleName(), e.getLocalizedMessage());
            return null;
        }
    }

    private static String getJsonString(OpenApiViolationAggregator oas) throws JsonProcessingException {
        var yamlReader = new ObjectMapper(new YAMLFactory());
        var obj = yamlReader.readValue(oas.getSrc().get(oas.getOpenApiFile().getName()).stream().collect(Collectors.joining("\n")), Object.class);
        return new ObjectMapper().writeValueAsString(obj);
    }


    private void parsePaths(Paths paths, ParserResult result) {
        if (paths == null) {
            return;
        }
        Map<String, PathItem> pathItems = paths.getPathItems();
        pathItems.forEach((path, pathitem) -> {
            if (pathitem.getOperations() != null) {
                pathitem.getOperations().forEach((method, operation) -> {
                    var operationDef = new OperationDefinition(operation, path, method, openApiFile);
                    result.operations.add(operationDef);
                    parseOperation(operationDef, result);
                });
            }
            if (pathitem.getParameters() != null) {
                pathitem.getParameters().forEach(parameter -> {
                    var paramDef = new ParameterDefinition(parameter, path, openApiFile, "/paths/" + path + "/parameters/" + parameter.getName());
                    result.parameters.add(paramDef);
                    parseParameter(paramDef, result);
                });
            }
        });
    }

    private void parseParameter(ParameterDefinition parameterDefinition, ParserResult result) {
        Parameter param = parameterDefinition.getModel();
        if (param != null && param.getRef() == null) {
            if (param.getSchema() != null && param.getSchema().getRef() == null) {
                var schemaDefinition = new SchemaDefinition(param.getSchema(), parameterDefinition, param.getSchema().getTitle());
                result.schemas.add(schemaDefinition);
            }
            if (param.getContent() != null && param.getContent().getMediaTypes() != null) {
                Map<String, MediaType> mediaTypes = param.getContent().getMediaTypes();
                mediaTypes.forEach((mediaType, mediaTypeObject) -> {
                    var mediaTypeDef = new MediaTypeDefinition(mediaTypeObject, parameterDefinition, mediaType);
                    result.mediaTypes.add(mediaTypeDef);
                    parseMediaType(mediaTypeDef, result);
                });
            }
        }
    }

    private void parseOperation(OperationDefinition operationDef, ParserResult result) {
        RequestBody requestBody = operationDef.getModel().getRequestBody();
        if (requestBody != null && requestBody.getRef() == null) {
            var requestBodyDefinition = new RequestBodyDefinition(requestBody, operationDef);
            result.requestBodies.add(requestBodyDefinition);
            parseRequestBody(requestBodyDefinition, result);
        }

        List<Parameter> parameters = operationDef.getModel().getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                if (parameter.getRef() == null) {
                    var parameterDefinition = new ParameterDefinition(parameter, operationDef, parameter.getName(), "/parameters/" + parameter.getName());
                    result.parameters.add(parameterDefinition);
                    parseParameter(parameterDefinition, result);
                }
            }
        }

        APIResponses apiResponses = operationDef.getModel().getResponses();
        if (apiResponses != null) {
            var responses = apiResponses.getAPIResponses();
            responses.forEach((statusCode, responseObject) -> {
                if (responseObject.getRef() == null) {
                    var responseDef = new ResponseDefinition(responseObject, operationDef, statusCode);
                    result.responses.add(responseDef);
                    parseResponse(responseDef, result);
                }
            });
        }
    }

    public void parseComponents(Components components, ParserResult result) {
        if (components == null) {
            return;
        }
        Map<String, APIResponse> responses = components.getResponses();
        if (responses != null) {
            responses.forEach((name, response) -> {
                var responseDef = new ResponseDefinition(response, name, openApiFile);
                result.responses.add(responseDef);
                parseResponse(responseDef, result);
            });
        }

        Map<String, RequestBody> requestBodies = components.getRequestBodies();
        if (requestBodies != null) {
            requestBodies.forEach((name, requestBody) -> {
                var requestBodyDef = new RequestBodyDefinition(requestBody, name, openApiFile);
                result.requestBodies.add(requestBodyDef);
                parseRequestBody(requestBodyDef, result);
            });
        }

        Map<String, Schema> schemas = components.getSchemas();
        if (schemas != null) {
            schemas.forEach((name, schema) -> {
                var schemaDef = new SchemaDefinition(schema, name, openApiFile);
                result.schemas.add(schemaDef);
                parseSchema(schemaDef, result);
            });
        }

        Map<String, Header> headers = components.getHeaders();
        if (headers != null) {
            headers.forEach((name, header) -> {
                var headerDef = new ResponseHeaderDefinition(header, name, openApiFile);
                result.headers.add(headerDef);
                parseHeaders(headerDef, result);
            });
        }

    }

    public void parseRequestBody(RequestBodyDefinition requestBodyDef, ParserResult result) {
        var content = requestBodyDef.getModel().getContent();
        if (content != null) {
            var mediaTypes = content.getMediaTypes();
            mediaTypes.forEach((mediaType, mediaTypeObject) -> {
                var mediaTypeDef = new MediaTypeDefinition(mediaTypeObject, requestBodyDef, mediaType);
                result.mediaTypes.add(mediaTypeDef);
                parseMediaType(mediaTypeDef, result);
            });
        }
    }

    public void parseResponse(ResponseDefinition responseDef, ParserResult result) {
        var content = responseDef.getModel().getContent();
        if (content != null) {
            var mediaTypes = content.getMediaTypes();
            mediaTypes.forEach((mediaType, mediaTypeObject) -> {
                var mediaTypeDef = new MediaTypeDefinition(mediaTypeObject, responseDef, mediaType);
                result.mediaTypes.add(mediaTypeDef);
                parseMediaType(mediaTypeDef, result);
            });
        }
        var headers = responseDef.getModel().getHeaders();
        if (headers != null) {
            headers.forEach((name, header) -> {
                if (header.getRef() == null) {
                    var headerDef = new ResponseHeaderDefinition(header, responseDef, name);
                    result.headers.add(headerDef);
                    parseHeaders(headerDef, result);
                }
            });
        }
    }

    public void parseHeaders(ResponseHeaderDefinition responseHeaderDefinition, ParserResult result) {
        var schema = responseHeaderDefinition.getModel().getSchema();
        if (schema != null && schema.getRef() == null) {
            var schemaDef = new SchemaDefinition(schema, responseHeaderDefinition, schema.getTitle());
            result.schemas.add(schemaDef);
            parseSchema(schemaDef, result);
        } else if (responseHeaderDefinition.getModel().getContent() != null) {
            var mediaTypes = responseHeaderDefinition.getModel().getContent().getMediaTypes();
            if (mediaTypes != null) {
                mediaTypes.forEach((mediaType, mediaTypeObject) -> {
                    var mediaTypeDef = new MediaTypeDefinition(mediaTypeObject, responseHeaderDefinition, mediaType);
                    result.mediaTypes.add(mediaTypeDef);
                    parseMediaType(mediaTypeDef, result);
                });
            }
        }
    }

    public void parseMediaType(MediaTypeDefinition mediaTypeDefinition, ParserResult result) {
        var schema = mediaTypeDefinition.getModel().getSchema();
        if (schema != null) {
            if (schema.getRef() == null) {
                var schemaDef = new SchemaDefinition(schema, mediaTypeDefinition, schema.getTitle());
                result.schemas.add(schemaDef);
                parseSchema(schemaDef, result);
            }
        }
    }

    public void parseSchema(SchemaDefinition schemaDefinition, ParserResult result) {
        var parentSchema = schemaDefinition.getModel();
        constructNestedSchema(parentSchema.getAllOf(), "/allOf", schemaDefinition, result);
        constructNestedSchema(parentSchema.getOneOf(), "/oneOf", schemaDefinition, result);
        constructNestedSchema(parentSchema.getAnyOf(), "/anyOf", schemaDefinition, result);
        constructNestedSchema(parentSchema.getAdditionalPropertiesSchema(), "/additionalProperties", schemaDefinition, result);
        constructNestedSchema(parentSchema.getItems(), "/items", schemaDefinition, result);
        if (parentSchema.getProperties() != null) {
            parentSchema.getProperties().forEach((propertyName, propertyObject) ->
                    constructNestedSchema(propertyObject, "/properties/" + propertyName, schemaDefinition, result));
        }
    }

    private void constructNestedSchema(List<Schema> schemas, String namePrefix, SchemaDefinition parentSchema, ParserResult result) {
        if (schemas != null) {
            int index = 0;
            while (index < schemas.size()) {
                constructNestedSchema(schemas.get(index), namePrefix + "/" + index, parentSchema, result);
                index++;
            }
        }
    }

    private void constructNestedSchema(Schema schema, String namePrefix, SchemaDefinition parentSchema, ParserResult result) {
        if (schema != null && schema.getRef() == null) {
            var schemaDef = new SchemaDefinition(schema, parentSchema, schema.getTitle(), namePrefix);
            result.schemas.add(schemaDef);
            parseSchema(schemaDef, result);
        }
    }


}
