package be.belgium.gcloud.rest.styleguide.validation.core.parser;

import be.belgium.gcloud.rest.styleguide.validation.LineRangePath;
import be.belgium.gcloud.rest.styleguide.validation.core.ApiFunctions;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.models.*;
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
        private Set<OpenApiDefinition<? extends Constructible>> allDefinitions = new HashSet<>();

        public String jsonString;
        private OpenAPI openAPI;
        private List<LineRangePath> paths;

        private void assembleAllDefinitions() {
            allDefinitions.addAll(requestBodies);
            allDefinitions.addAll(responses);
            allDefinitions.addAll(operations);
            allDefinitions.addAll(mediaTypes);
        }

        public <T extends Constructible> OpenApiDefinition<T> resolve(T model) {
            if (model instanceof Reference) {
                String ref = ((Reference) model).getRef();
                if (ref != null) {
                    String id = getRefName(ref); //possible improvement: to avoid problems with naming collisions, also take type into account when resolve (response, requestBody, ...)
                    var defMatch = allDefinitions.stream().filter(def -> def.getIdentifier() == id).findAny();
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
        var obj = yamlReader.readValue(oas.getSrc().stream().collect(Collectors.joining("\n")), Object.class);
        return new ObjectMapper().writeValueAsString(obj);
    }

    private void parsePaths(Paths paths, ParserResult result) {
        if (paths == null) {
            return;
        }
        Map<String, PathItem> pathItems = paths.getPathItems();
        pathItems.forEach((path, pathitem) -> {
            pathitem.getOperations().forEach((method, operation) -> {
                var operationDef = new OperationDefinition(operation, path, method, openApiFile);
                result.operations.add(operationDef);
                parseOperation(operationDef, result);
            });
        });
    }

    private void parseOperation(OperationDefinition operationDef, ParserResult result) {
        RequestBody requestBody = operationDef.getModel().getRequestBody();
        if (requestBody != null && requestBody.getRef() == null) {
            var requestBodyDefinition = new RequestBodyDefinition(requestBody, operationDef);
            result.requestBodies.add(requestBodyDefinition);
            parseRequestBody(requestBodyDefinition, result);
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
    }

    public void parseRequestBody(RequestBodyDefinition requestBodyDef, ParserResult result) {
        var content = requestBodyDef.getModel().getContent();
        if (content != null) {
            var mediaTypes = content.getMediaTypes();
            mediaTypes.forEach((mediaType, mediaTypeObject) -> {
                result.mediaTypes.add(new MediaTypeDefinition(mediaTypeObject, requestBodyDef, mediaType));
            });
        }
    }

    public void parseResponse(ResponseDefinition responseDef, ParserResult result) {
        var content = responseDef.getModel().getContent();
        if (content != null) {
            var mediaTypes = content.getMediaTypes();
            mediaTypes.forEach((mediaType, mediaTypeObject) -> {
                result.mediaTypes.add(new MediaTypeDefinition(mediaTypeObject, responseDef, mediaType));
            });
        }
    }


}
