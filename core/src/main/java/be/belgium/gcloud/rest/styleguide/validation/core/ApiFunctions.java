package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.LineRangePath;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.*;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.openapitools.empoa.swagger.core.internal.SwAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class ApiFunctions {

    /**
     * avoid instance creation.
     */
    private ApiFunctions() {
    }

    /**
     * Read a file and store each line in a list.
     * If the file has only one line this function use a GSon librairy to build a pretty list of line.
     *
     * @param file must be a yaml or a json file
     * @return a list of line
     * @throws IOException
     */
    private static List<String> getLines(File file) throws IOException {
        var lines = Files.readAllLines(file.toPath());

        // lines > 1 then is a yaml or a pretty json file
        if (lines.size() > 1)
            return lines;

        // else is a ugly json file
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var pretty = gson.toJson(JsonParser.parseString(lines.get(0)));
        return pretty.lines().collect(Collectors.toList());
    }

    /**
     * Build the java object structure from the file.
     *
     * @param file a openApi yaml or json file.
     * @param oas  OpenApiViolationAggregator used to add file and list of line from the file.
     * @return OpenAPI, the java object structure
     * @throws IOException
     * @side-effect add the file and a list of line from the file to oas.
     */
    public static OpenAPI buildOpenApiSpecification(File file, OpenApiViolationAggregator oas) throws IOException {
        oas.setOpenApiFile(file);
        oas.setSrc(getLines(file));

        var openApiParser = new OpenAPIParser();
        var parseOptions = new ParseOptions();
        parseOptions.setResolve(true);

        var parserResult = openApiParser.readLocation(file.getAbsolutePath(), null, parseOptions);
        var openAPI = parserResult.getOpenAPI();

        // parser return a null value when the spec version is not 2.x -3.0.x
        // and SwAdapter raise a silent JsonParserException
        // if spec is 3.1 we need the info to raise a rule violation
        if (openAPI == null) {
            openAPI = new io.swagger.v3.oas.models.OpenAPI();
            var version = getLines(file).stream()
                    .filter(line -> line.trim().startsWith("openapi: "))
                    .findFirst().orElseThrow()
                    .substring(9);
            openAPI.setOpenapi(version);
        }
        return SwAdapter.toOpenAPI(openAPI);
    }


    /**
     * In the openAPI get all Path keys that match the pattern.
     *
     * @param openAPI
     * @param pattern
     * @return a set path keys
     */
    public static Set<String> getPathMatch(OpenAPI openAPI, String pattern) {
        return getPathKeys(openAPI).stream().filter(k -> k.matches(pattern)).collect(Collectors.toSet());
    }

    /**
     * In the openAPI get all Path keys that NOT match the pattern.
     *
     * @param openAPI
     * @param pattern
     * @return a set path keys
     */
    public static Set<String> getPathNoMatch(OpenAPI openAPI, String pattern) {
        return getPathKeys(openAPI).stream().filter(k -> !k.matches(pattern)).collect(Collectors.toSet());
    }

    /**
     * Return all path key for openAPI
     *
     * @param openAPI
     * @return a set path keys
     */
    public static Set<String> getPathKeys(OpenAPI openAPI) {
        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems() == null)
            return Collections.emptySet();
        return openAPI.getPaths().getPathItems().keySet();
    }

    /**
     * Return all path for openAPI
     *
     * @param api
     * @return
     */
    public static Paths getPaths(OpenAPI api) {
        return api.getPaths();
    }

    /**
     * For the openapi get all path.operationId that match the verb and the status code.
     *
     * @param openAPI
     * @param verb
     * @param statusCode
     * @return
     */
    public static Set<String> getOperationId(OpenAPI openAPI, OperationEnum verb, String statusCode) {
        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems() == null)
            return Collections.emptySet();
        return openAPI.getPaths().getPathItems().values().stream()
                .filter(path -> filterPath(path, verb, statusCode))
                .map(path -> getOperationId(path, verb))
                .collect(Collectors.toSet());
    }

    private static boolean filterPath(PathItem path, OperationEnum verb, String statusCode) {
        switch (verb) {
            case GET:
                return path.getGET() != null && path.getGET().getResponses().getAPIResponses().containsKey(statusCode);
            case POST:
                return path.getPOST() != null && path.getPOST().getResponses().getAPIResponses().containsKey(statusCode);
            case PUT:
                return path.getPUT() != null && path.getPUT().getResponses().getAPIResponses().containsKey(statusCode);
            case DELETE:
                return path.getDELETE() != null && path.getDELETE().getResponses().getAPIResponses().containsKey(statusCode);
            case PATCH:
                return path.getPATCH() != null && path.getPATCH().getResponses().getAPIResponses().containsKey(statusCode);
            case HEAD:
                return path.getHEAD() != null && path.getHEAD().getResponses().getAPIResponses().containsKey(statusCode);
            case OPTIONS:
                return path.getOPTIONS() != null && path.getOPTIONS().getResponses().getAPIResponses().containsKey(statusCode);
            default:
                throw new IllegalArgumentException("unknow verb: " + verb);
        }
    }

    private static String getOperationId(PathItem path, OperationEnum verb) {
        switch (verb) {
            case GET:
                return path.getGET().getOperationId();
            case POST:
                return path.getPOST().getOperationId();
            case PUT:
                return path.getPUT().getOperationId();
            case DELETE:
                return path.getDELETE().getOperationId();
            case PATCH:
                return path.getPATCH().getOperationId();
            case HEAD:
                return path.getHEAD().getOperationId();
            case OPTIONS:
                return path.getOPTIONS().getOperationId();
            default:
                throw new IllegalArgumentException("unknow verb: " + verb);
        }
    }


    /**
     * For openAPI, get all server.url that NOT match the regex.
     *
     * @param openAPI
     * @param regex
     * @return
     */
    public static Set<String> getServerNotMatch(OpenAPI openAPI, String regex) {
        if (openAPI.getServers() == null)
            return Collections.emptySet();
        return openAPI.getServers().stream()
                .map(Server::getUrl)
                .filter(url -> !url.matches(regex))
                .collect(Collectors.toSet());
    }

    /**
     * Get all properties defined anywhere in the OpenAPI
     */
    public static Set<PropertyDefinition> getProperties(OpenAPI api) {
        Set<PropertyDefinition> properties = new HashSet<>();
        getSchemas(api).forEach(schemaDef -> {
            if (schemaDef.getSchema().getProperties() != null) {
                schemaDef.getSchema().getProperties().forEach((k, v) ->
                        properties.add(new PropertyDefinition(schemaDef.getParentDefinitionLocation(), schemaDef.getParentName(), k, v)));
            }
        });
        return properties;
    }

    /**
     * For all openAPI.path.key build a LineRangePath whith the start and end line of the path.
     *
     * @param openAPI
     * @param oas
     * @return
     * @pre: Last path in openAPI, does not have a 'real' end-line, last line of file is set as end.
     */
    public static List<LineRangePath> buildAllPathWithLineRange(OpenAPI openAPI, OpenApiViolationAggregator oas) {
        var paths = new ArrayList<LineRangePath>();
        var pathKeys = getPathKeys(openAPI);

        if (pathKeys.isEmpty())
            return Collections.emptyList();

        pathKeys.forEach(p -> paths.add(new LineRangePath(p, oas.getLineNumber(p))));
        Collections.sort(paths);
        for (int i = 0; i < pathKeys.size() - 1; i++) {
            paths.get(i).setEnd(paths.get(i + 1).getStart() - 1);
        }

        var last = paths.get(paths.size() - 1);

        last.setEnd(oas.src.size() - 1);

        return paths;
    }

    /**
     * Return true if at least one element that {LineRangePath.path == path and LineRangePath.start >= lineNumber < LineRangePath.end}
     *
     * @param lineRangePaths
     * @param path
     * @param lineNumber
     * @return
     */
    public static boolean isInPathList(List<LineRangePath> lineRangePaths, String path, int lineNumber) {
        return lineRangePaths.stream()
                .anyMatch(lineRangePath -> lineRangePath.getPath().equals(path) && lineRangePath.inRange(lineNumber));
    }

    /**
     * Return true if at least one element that {LineRangePath.path IN paths and LineRangePath.start >= lineNumber < LineRangePath.end}
     *
     * @param lineRangePaths
     * @param paths
     * @param lineNumber
     * @return
     */
    public static boolean isInPathList(List<LineRangePath> lineRangePaths, List<String> paths, int lineNumber) {
        return lineRangePaths.stream()
                .anyMatch(lineRangePath -> paths.contains(lineRangePath.getPath()) && lineRangePath.inRange(lineNumber));
    }

    /**
     * Get a list of pathkey that return a collection.
     *
     * @param openAPI
     * @return
     */
    public static List<String> getReturnCollectionPathKey(OpenAPI openAPI) {
        return new ArrayList<>(getCollectionPathItems(openAPI).keySet());
    }

    public static Map<String, PathItem> getCollectionPathItems(OpenAPI openAPI) {
        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems().isEmpty()) {
            return new HashMap<>();
        }
        var allPaths = openAPI.getPaths().getPathItems().entrySet();
        // Adds all paths before the ones with path params
        Set<String> collectionPaths = allPaths.stream().filter(path -> endsWithPathParameter(path.getKey()))
                .map(path -> getPathBeforePathParam(path, openAPI)).filter(Objects::nonNull).collect(Collectors.toSet());
        // Adds all paths that return an object with an array 'items'
        collectionPaths.addAll(allPaths.stream().filter(path -> isReturnCollection(openAPI, path.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet()));

        // Filter out all collections without GET
        return allPaths.stream()
                .filter(path -> collectionPaths.contains(path.getKey()) &&
                        path.getValue().getOperations().containsKey(PathItem.HttpMethod.GET))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static String getPathBeforePathParam(Map.Entry<String, PathItem> path, OpenAPI openAPI) {
        String pathString = path.getKey();
        if (pathString.endsWith("}") || pathString.endsWith("}/")) {
            Pattern pattern = Pattern.compile("(^/.*)/\\{.*}$");
            Matcher matcher = pattern.matcher(pathString);
            if (matcher.find()) {
                String pathBeforePathParam = matcher.group(1);
                if (pathBeforePathParam != null && openAPI.getPaths().hasPathItem(pathBeforePathParam)) {
                    return pathBeforePathParam;
                }
            }
        }
        return null;
    }

    private static boolean endsWithPathParameter(String path) {
        return path != null && path.endsWith("}");
    }

    public static boolean isReturnCollection(OpenAPI openAPI, PathItem pathItem) {
        try {
            AtomicBoolean isCollection = new AtomicBoolean(false);
            var responseSchemas = pathItem.getGET().getResponses().getAPIResponses().values().stream()
                    .flatMap(apiResponse -> apiResponse.getContent().getMediaTypes().values().stream())
                    .map(MediaType::getSchema);
            responseSchemas.forEach(schema -> {
                if (schema.getProperties() != null && schema.getProperties().containsKey("items")
                        && schema.getProperties().get("items").getType().equals(Schema.SchemaType.ARRAY)) {
                    isCollection.set(true);
                } else {
                    if (isCollection(openAPI, schema.getRef()))
                        isCollection.set(true);
                }
            });
            return isCollection.get();
        } catch (NullPointerException ex) {
            return false;
        }
    }

    private static boolean isCollection(OpenAPI openAPI, String ref) {
        try {
            if (!ref.startsWith("#")) {
                log.debug("Cannot check an external reference.");
                return false;
            }
            return openAPI.getComponents().getSchemas().get(getRefName(ref)).getProperties().get("items").getType().equals(Schema.SchemaType.ARRAY);

        } catch (NullPointerException ex) {
            return false;
        }
    }

    public static Set<Operation> getOperations(OpenAPI api) {
        return getOperations(api, Set.of());
    }

    public static Set<Operation> getOperations(OpenAPI api, Set<OperationEnum> exclude) {
        Collection<PathItem> pathItems = api.getPaths().getPathItems().values();
        Set<Operation> operations = new HashSet<>();
        for (PathItem pathItem : pathItems) {
            if (pathItem.getGET() != null && !exclude.contains(OperationEnum.GET)) {
                operations.add(pathItem.getGET());
            }
            if (pathItem.getPUT() != null && !exclude.contains(OperationEnum.PUT)) {
                operations.add(pathItem.getPUT());
            }
            if (pathItem.getPOST() != null && !exclude.contains(OperationEnum.POST)) {
                operations.add(pathItem.getPOST());
            }
            if (pathItem.getDELETE() != null && !exclude.contains(OperationEnum.DELETE)) {
                operations.add(pathItem.getDELETE());
            }
            if (pathItem.getPATCH() != null && !exclude.contains(OperationEnum.PATCH)) {
                operations.add(pathItem.getPATCH());
            }
            if (pathItem.getHEAD() != null && !exclude.contains(OperationEnum.HEAD)) {
                operations.add(pathItem.getHEAD());
            }
            if (pathItem.getOPTIONS() != null && !exclude.contains(OperationEnum.OPTIONS)) {
                operations.add(pathItem.getOPTIONS());
            }
        }
        return operations;
    }

    private static String getRefName(String ref) {
        if (!ref.contains("/"))
            return ref;
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    public static Set<Schema> getSchemaFromContent(OpenAPI api, Content content, Set<String> contentTypes) {
        List<org.springframework.http.MediaType> mediaTypeList = new ArrayList<>();
        for (String contentType : contentTypes) {
            mediaTypeList.add(org.springframework.http.MediaType.parseMediaType(contentType));
        }
        Set<Schema> schemas = content.getMediaTypes().entrySet().stream().filter(set -> isMediaTypeIncluded(set.getKey(), mediaTypeList)).map(set -> set.getValue().getSchema()).collect(Collectors.toSet());
        Set<Schema> outputSchemas = new HashSet<>();
        for (Schema schema : schemas) {
            if (schema.getType() == null && schema.getRef() != null) {
                Schema refSchema = getReferenceSchema(api, schema.getRef());
                outputSchemas.add(refSchema);
            } else {
                outputSchemas.add(schema);
            }
        }
        return outputSchemas;
    }

    private static Schema getReferenceSchema(OpenAPI api, String ref) {
        // input files are pre-merged by swagger-parser setResolve(true), so all references should be in the parsed OpenAPI model
        if (api.getComponents() == null || api.getComponents().getSchemas() == null) {
            throw new IllegalStateException("Input OpenAPI file is invalid. Could not resolve reference " + ref + ". /components/schemas is missing.");
        }
        Schema resolvedSchema = api.getComponents().getSchemas().get(getRefName(ref));
        if (resolvedSchema == null) {
            throw new IllegalStateException("Input OpenAPI file is invalid. Could not resolve reference to schema: " + ref + ", or reference does not point to schema.");
        }
        return resolvedSchema;
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

    public static Set<Parameter> getParameters(OpenAPI api) {
        Set<Parameter> parameters = new HashSet<>();
        api.getPaths().getPathItems().values().forEach(pathItem -> {
            if (pathItem.getParameters() != null) {
                parameters.addAll(pathItem.getParameters());
            }
        });
        getOperations(api).forEach(operation -> {
            if (operation.getParameters() != null) {
                parameters.addAll(operation.getParameters());
            }
        });
        return parameters;
    }

    public static Set<SchemaDefinition> getSchemas(OpenAPI api) {
        Set<SchemaDefinition> schemas = new HashSet<>();
        Components components = api.getComponents();
        if (components != null) {
            // Gets schemas from top-level components
            if (components.getSchemas() != null) {
                components.getSchemas().forEach((schemaName, schema) -> schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.COMPONENT_SCHEMAS, schemaName, schema)));
            }
            schemas.addAll(getResponseSchemas(components));
            schemas.addAll(getRequestBodySchemas(components));
            schemas.addAll(getHeaderSchemas(components));
            schemas.addAll(getCallbackSchemas(components));
            schemas.addAll(getTopLevelParameterSchemas(components));
        }
        // Gets schemas from parameters declared on PathItem level
        Set<Parameter> parameters = new HashSet<>();
        api.getPaths().getPathItems().values().stream().filter(pathItem -> pathItem.getParameters() != null).forEach(pathItem -> parameters.addAll(pathItem.getParameters()));
        parameters.forEach(parameter ->
                schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.PARAMETER, parameter.getName(), parameter.getSchema()))
        );
        // Get schemas from operations
        getOperations(api).forEach(operation ->
                schemas.addAll(getSchemasFromOperation(operation))
        );

        Set<SchemaDefinition> nestedSchemas = new HashSet<>();
        schemas.forEach(schema -> nestedSchemas.addAll(getNestedSchemas(schema)));
        schemas.addAll(nestedSchemas);

        return schemas.stream().filter(schemaDefinition -> schemaDefinition.getSchema().getRef() == null).collect(Collectors.toSet());
    }

    private static Set<SchemaDefinition> getCallbackSchemas(Components components) {
        Set<SchemaDefinition> schemas = new HashSet<>();
        if (components.getCallbacks() != null) {
            components.getCallbacks().forEach((callbackName, callback) ->
                    callback.getPathItems().forEach((pathItemName, pathItem) ->
                            pathItem.getOperations().forEach(((httpMethod, operation) ->
                                    schemas.addAll(getSchemasFromOperation(operation))
                            ))
                    )
            );
        }

        return schemas;
    }

    private static Set<SchemaDefinition> getRequestBodySchemas(Components components) {
        Set<SchemaDefinition> schemas = new HashSet<>();
        // Get top level requestBodies
        if (components.getRequestBodies() != null) {
            components.getRequestBodies().forEach((requestName, request) -> {
                if (request.getContent() != null && request.getContent().getMediaTypes() != null) {
                    request.getContent().getMediaTypes().forEach((mediaTypeName, mediaType) -> {
                        if (mediaType.getSchema() != null) {
                            schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.COMPONENT_REQUESTBODY, requestName + ":" + mediaTypeName, mediaType.getSchema()));
                        }
                    });
                }
            });
        }
        return schemas;
    }

    private static Set<SchemaDefinition> getHeaderSchemas(Components components) {
        Set<SchemaDefinition> schemas = new HashSet<>();
        // Get top level header schemas
        if (components.getHeaders() != null) {
            components.getHeaders().forEach((headerName, header) -> {
                if (header.getSchema() != null) {
                    schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.COMPONENT_HEADER, headerName, header.getSchema()));
                }
            });
        }
        return schemas;
    }

    private static Set<SchemaDefinition> getTopLevelParameterSchemas(Components components) {
        Set<SchemaDefinition> schemas = new HashSet<>();
        if (components.getParameters() != null) {
            components.getParameters().forEach((parameterName, parameter) ->
                    schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.PARAMETER, parameterName, parameter.getSchema()))
            );
        }
        return schemas;
    }

    private static Set<SchemaDefinition> getResponseSchemas(Components components) {
        Set<SchemaDefinition> schemas = new HashSet<>();
        // Get top level responses
        if (components.getResponses() != null) {
            components.getResponses().forEach((responseName, response) -> {
                if (response.getContent().getMediaTypes() != null) {
                    response.getContent().getMediaTypes().forEach((mediaTypeName, mediaType) -> {
                        if (mediaType.getSchema() != null) {
                            schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.COMPONENT_RESPONSES, responseName + ":" + mediaTypeName, mediaType.getSchema()));
                        }
                    });
                }
                if (response.getHeaders() != null) {
                    response.getHeaders().forEach((headerName, header) -> {
                        if (header.getSchema() != null) {
                            schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.INLINE_HEADER, responseName + ":" + headerName, header.getSchema()));
                        }
                    });
                }
            });
        }
        return schemas;
    }

    private static Set<SchemaDefinition> getSchemasFromOperation(Operation operation) {
        Set<SchemaDefinition> schemas = new HashSet<>();
        // Get responses
        if (operation.getResponses() != null && operation.getResponses().getAPIResponses() != null) {
            operation.getResponses().getAPIResponses().forEach((responseName, response) -> {
                if (response.getContent() != null && response.getContent().getMediaTypes() != null) {
                    response.getContent().getMediaTypes().forEach((mediaTypeName, mediaType) ->
                            schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.INLINE_RESPONSES, operation.getOperationId() + ":" + responseName + ":" + mediaTypeName, mediaType.getSchema()))
                    );
                }
                if (response.getHeaders() != null) {
                    response.getHeaders().forEach((headerName, header) -> {
                        if (header.getSchema() != null) {
                            schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.INLINE_HEADER, operation.getOperationId() + ":" + responseName + ":" + headerName, header.getSchema()));
                        }
                    });
                }
            });
        }
        // Get requestbodies
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null && operation.getRequestBody().getContent().getMediaTypes() != null) {
            operation.getRequestBody().getContent().getMediaTypes().forEach((mediaTypeName, mediaType) -> {
                if (mediaType.getSchema() != null) {
                    schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.INLINE_REQUESTBODY, operation.getOperationId() + ":" + mediaTypeName, mediaType.getSchema()));
                }
            });
        }
        // Get parameters
        if (operation.getParameters() != null) {
            Set<Parameter> parameters = new HashSet<>(operation.getParameters());
            parameters.forEach(parameter ->
                    schemas.add(new SchemaDefinition(OpenApiDefinitionLocation.PARAMETER, parameter.getName(), parameter.getSchema()))
            );
        }
        // Get callbacks
        if (operation.getCallbacks() != null) {
            operation.getCallbacks().forEach((callbackName, callback) ->
                    callback.getPathItems().forEach((pathItemName, pathItem) ->
                            pathItem.getOperations().forEach((httpMethod, callbackOperation) ->
                                    schemas.addAll(getSchemasFromOperation(callbackOperation))
                            )
                    )
            );
        }

        return schemas;
    }

    private static Set<SchemaDefinition> getNestedSchemas(SchemaDefinition parentSchemaDefinition) {
        Set<SchemaDefinition> schemas = new HashSet<>();
        var parentSchema = parentSchemaDefinition.getSchema();
        if (parentSchema.getProperties() != null) {
            parentSchema.getProperties().forEach((schemaName, schema) ->
                    {
                        var schemaOfProperty = new SchemaDefinition(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":" + schemaName, schema);
                        schemas.add(schemaOfProperty);
                        schemas.addAll(getNestedSchemas(schemaOfProperty));
                    }
            );
        }
        if (parentSchema.getItems() != null) {
            var schemaOfProperty = new SchemaDefinition(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":items", parentSchema.getItems());
            schemas.add(schemaOfProperty);
            schemas.addAll(getNestedSchemas(schemaOfProperty));
        }
        if (parentSchema.getAllOf() != null) {
            parentSchema.getAllOf().forEach(schema -> {
                var schemaOfProperty = new SchemaDefinition(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":allOf", schema);
                schemas.add(schemaOfProperty);
                schemas.addAll(getNestedSchemas(schemaOfProperty));
            });
        }
        if (parentSchema.getAnyOf() != null) {
            parentSchema.getAnyOf().forEach(schema -> {
                var schemaOfProperty = new SchemaDefinition(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":anyOf", schema);
                schemas.add(schemaOfProperty);
                schemas.addAll(getNestedSchemas(schemaOfProperty));
            });
        }
        if (parentSchema.getOneOf() != null) {
            parentSchema.getOneOf().forEach(schema -> {
                var schemaOfProperty = new SchemaDefinition(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":oneOf", schema);
                schemas.add(schemaOfProperty);
                schemas.addAll(getNestedSchemas(schemaOfProperty));
            });
        }
        if (parentSchema.getAdditionalPropertiesSchema() != null) {
            var schemaOfProperty = new SchemaDefinition(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":additionalProperties", parentSchema.getAdditionalPropertiesSchema());
            schemas.add(schemaOfProperty);
            schemas.addAll(getNestedSchemas(schemaOfProperty));
        }
        return schemas;
    }

    public static boolean isLowerCamelCase(String string) {
        return string.matches("^[a-z0-9]+([A-Z]?[a-z0-9]+)*$");
    }

}