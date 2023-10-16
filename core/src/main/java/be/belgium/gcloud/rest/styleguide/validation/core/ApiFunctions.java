package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.LineRangePath;
import be.belgium.gcloud.rest.styleguide.validation.core.model.OpenApiDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.model.PathDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.model.SchemaDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import java.nio.file.Path;
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
        if (lines.size() > 1) return lines;

        // else is a ugly json file
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var pretty = gson.toJson(JsonParser.parseString(lines.get(0)));
        return pretty.lines().collect(Collectors.toList());
    }

    public static Set<File> getReferencedFiles(File file) throws IOException {
        Set<File> refFiles = new HashSet<>();
        resolveReferences(file, refFiles);
        return refFiles;
    }

    private static void resolveReferences(File file, Set<File> files) throws IOException {
        Path basePath = java.nio.file.Paths.get(file.getAbsolutePath().split(file.getName())[0]);
        Set<String> refs = getExternalReferencesFromFile(file);
        for (String ref : refs) {
            File refFile = resolveFileFromRef(ref, basePath);
            if (files.add(refFile)) {
                resolveReferences(refFile, files);
            }
        }
    }

    private static File resolveFileFromRef(String ref, Path basePath) {
        File refFile = new File(String.valueOf(basePath.resolve(ref).normalize()));
        if (refFile.exists() && refFile.isFile()) {
            return refFile;
        } else {
            throw new RuntimeException("File not found: " + refFile.getAbsolutePath());
        }
    }

    private static Set<String> getExternalReferencesFromFile(File file) throws IOException {
        Set<String> references = new HashSet<>();
        ObjectMapper mapper;

        if (file.getName().endsWith("yaml") || file.getName().endsWith("yml")) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else {
            mapper = new ObjectMapper();
        }

        JsonNode jsonNode = mapper.readTree(file);
        findRefFields(jsonNode, references);
        return references;
    }

    private static boolean isExternalReference(String ref) {
        return !ref.startsWith("#");
    }

    private static void findRefFields(JsonNode node, Set<String> refs) {
        if (node.isObject()) {
            var fields = node.fields();
            fields.forEachRemaining(field -> {
                if (field.getKey().equals("$ref")) {
                    String ref = field.getValue().textValue();
                    if (isExternalReference(ref)) {
                        refs.add(ref.split("#")[0]);
                    }
                } else {
                    findRefFields(field.getValue(), refs);
                }
            });
        }
        if (node.isArray()) {
            var arrayField = (ArrayNode) node;
            arrayField.forEach(field -> {
                findRefFields(field, refs);
            });
        }
    }


    public static Map<String, List<String>> getAllLines(File file) throws IOException {
        Map<String, List<String>> allLines = new HashMap<>();
        allLines.put(file.getName(), getLines(file));
        Set<File> refFiles = getReferencedFiles(file);
        for (File refFile : refFiles) {
            allLines.put(refFile.getName(), getLines(refFile));
        }

        return allLines;
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
        oas.setSrc(getAllLines(file));

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
            var version = getLines(file).stream().filter(line -> line.trim().startsWith("openapi: ")).findFirst().orElseThrow().substring(9);
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
     * Return all path key for openAPI
     *
     * @param openAPI
     * @return a set path keys
     */
    public static Set<String> getPathKeys(OpenAPI openAPI) {
        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems() == null) return Collections.emptySet();
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
    public static Set<String> getOperationId(OpenAPI openAPI, PathItem.HttpMethod verb, String statusCode) {
        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems() == null) return Collections.emptySet();
        return openAPI.getPaths().getPathItems().values().stream().filter(path -> filterPath(path, verb, statusCode)).map(path -> getOperationId(path, verb)).collect(Collectors.toSet());
    }

    private static boolean filterPath(PathItem path, PathItem.HttpMethod verb, String statusCode) {
        return path.getOperations() != null && path.getOperations().get(verb) != null && path.getOperations().get(verb).getResponses() != null && path.getOperations().get(verb).getResponses().getAPIResponses().containsKey(statusCode);
    }

    private static String getOperationId(PathItem path, PathItem.HttpMethod verb) {
        return path.getOperations().get(verb).getOperationId();
    }


    /**
     * For openAPI, get all server.url that NOT match the regex.
     *
     * @param openAPI
     * @param regex
     * @return
     */
    public static Set<String> getServerNotMatch(OpenAPI openAPI, String regex) {
        if (openAPI.getServers() == null) return Collections.emptySet();
        return openAPI.getServers().stream().map(Server::getUrl).filter(url -> !url.matches(regex)).collect(Collectors.toSet());
    }

    /**
     * Get all properties defined anywhere in the OpenAPI
     */
    public static Set<PropertyDefinition> getProperties(OpenAPI api) {
        Set<PropertyDefinition> properties = new HashSet<>();
        getSchemas(api).forEach(schemaDef -> {
            if (schemaDef.getSchema().getProperties() != null) {
                schemaDef.getSchema().getProperties().forEach((k, v) -> properties.add(new PropertyDefinition(schemaDef.getParentDefinitionLocation(), schemaDef.getParentName(), k, v)));
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

        if (pathKeys.isEmpty()) return Collections.emptyList();

        pathKeys.forEach(p -> paths.add(new LineRangePath(p, oas.getLineNumber(p).getLineNumber())));
        Collections.sort(paths);
        for (int i = 0; i < pathKeys.size() - 1; i++) {
            paths.get(i).setEnd(paths.get(i + 1).getStart() - 1);
        }

        var last = paths.get(paths.size() - 1);

        last.setEnd(oas.src.get(oas.getLineNumber(last.getPath()).getFileName()).size() - 1);

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
        return lineRangePaths.stream().anyMatch(lineRangePath -> lineRangePath.getPath().equals(path) && lineRangePath.inRange(lineNumber));
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
        return lineRangePaths.stream().anyMatch(lineRangePath -> paths.contains(lineRangePath.getPath()) && lineRangePath.inRange(lineNumber));
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
        //TODO Going to be deprecated
        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems().isEmpty()) {
            return new HashMap<>();
        }
        var allPaths = openAPI.getPaths().getPathItems().entrySet();
        // Adds all paths before the ones with path params
        Set<String> collectionPaths = allPaths.stream().filter(path -> endsWithPathParameter(path.getKey())).map(path -> getPathBeforePathParam(path, openAPI)).filter(Objects::nonNull).collect(Collectors.toSet());
        // Adds all paths that return an object with an array 'items'
        collectionPaths.addAll(allPaths.stream().filter(path -> isReturnCollection(openAPI, path.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet()));

        // Filter out all collections without GET
        return allPaths.stream().filter(path -> collectionPaths.contains(path.getKey()) && path.getValue().getOperations().containsKey(PathItem.HttpMethod.GET)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    public static boolean hasPathWithPathParam(String pathString, Parser.ParserResult result) {
        Set<String> paths = result.getPathDefinitions().stream().filter(path -> path.getIdentifier().startsWith(pathString)).map(OpenApiDefinition::getIdentifier).collect(Collectors.toSet());
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

    private static boolean endsWithPathParameter(String path) {
        return path != null && path.endsWith("}");
    }

    public static boolean isReturnCollection(OpenAPI openAPI, PathItem pathItem) {
        try {
            AtomicBoolean isCollection = new AtomicBoolean(false);
            var responseSchemas = pathItem.getGET().getResponses().getAPIResponses().values().stream().flatMap(apiResponse -> apiResponse.getContent().getMediaTypes().values().stream()).map(MediaType::getSchema);
            responseSchemas.forEach(schema -> {
                if (schema.getProperties() != null && schema.getProperties().containsKey("items") && schema.getProperties().get("items").getType().equals(Schema.SchemaType.ARRAY)) {
                    isCollection.set(true);
                } else {
                    if (isCollection(openAPI, schema.getRef())) isCollection.set(true);
                }
            });
            return isCollection.get();
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public static boolean isCollection(PathDefinition path, Parser.ParserResult result) {
        AtomicBoolean isCollection = new AtomicBoolean(false);
        if (path.getModel().getGET() != null) {
            isCollection.set(hasPathWithPathParam(path.getIdentifier(), result));
        }
        if (!isCollection.get()) {
            try {
                var responses = path.getModel().getGET().getResponses().getAPIResponses().values().stream().flatMap(apiResponse -> apiResponse.getContent().getMediaTypes().values().stream()).map(MediaType::getSchema);
                responses.forEach(inlineSchema -> {
                    SchemaDefinition schemaDefinition = (SchemaDefinition) result.resolve(inlineSchema);
                    Schema schema = schemaDefinition.getModel();
                    if (schema.getProperties() != null && schema.getProperties().containsKey("items") && schema.getProperties().get("items").getType().equals(Schema.SchemaType.ARRAY)) {
                        isCollection.set(true);
                    }
                });
            } catch (NullPointerException ex) {
                return false;
            }
        }
        return isCollection.get();
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

    public static Set<Operation> getOperations(OpenAPI api, Set<PathItem.HttpMethod> exclude) {
        Collection<PathItem> pathItems = api.getPaths().getPathItems().values();
        Set<Operation> operations = new HashSet<>();
        for (PathItem pathItem : pathItems) {
            pathItem.getOperations().forEach((verb, operation) -> {
                if (!exclude.contains(verb)) {
                    operations.add(operation);
                }
            });
        }
        return operations;
    }

    private static String getRefName(String ref) {
        if (!ref.contains("/")) return ref;
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    public static Set<SchemaDefinitionDeprecated> getSchemaFromContent(OpenAPI api, Content content, Set<String> contentTypes) {
        List<org.springframework.http.MediaType> mediaTypeList = new ArrayList<>();
        for (String contentType : contentTypes) {
            mediaTypeList.add(org.springframework.http.MediaType.parseMediaType(contentType));
        }
        Set<Map.Entry<String, MediaType>> schemas = content.getMediaTypes().entrySet().stream().filter(set -> isMediaTypeIncluded(set.getKey(), mediaTypeList)).collect(Collectors.toSet());
        Set<SchemaDefinitionDeprecated> outputSchemas = new HashSet<>();
        for (Map.Entry<String, MediaType> mediaType : schemas) {
            if (mediaType.getValue() != null && mediaType.getValue().getSchema() != null) {
                if (mediaType.getValue().getSchema().getType() == null && mediaType.getValue().getSchema().getRef() != null) {
                    Schema refSchema = getReferenceSchema(api, mediaType.getValue().getSchema().getRef());
                    outputSchemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.COMPONENT_SCHEMAS, mediaType.getKey(), refSchema));
                } else {
                    outputSchemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.COMPONENT_SCHEMAS, mediaType.getKey(), mediaType.getValue().getSchema()));
                }
            }
        }
        return outputSchemas;
    }

    private static Schema getResolvedSchema(OpenAPI api, Schema schema) {
        if (schema.getRef() == null) {
            return schema;
        } else {
            Schema resolvedSchema = getReferenceSchema(api, schema.getRef());
            return getResolvedSchema(api, resolvedSchema);
        }
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

    /**
     * Returns true if schematype or any indirectly referenced type (allOf, anyOf, oneOf) is schemaType.
     * Returns false if schema allows more types than the specified one.
     *
     * @param api        The openapi specification
     * @param schema     Schema that has to be validated
     * @param schemaType type of the schema wanted, eg. object, string, array etc.
     */
    public static boolean isSchemaOfType(OpenAPI api, Schema schema, Schema.SchemaType schemaType) {
        Schema resolvedSchema = getResolvedSchema(api, schema);
        if (resolvedSchema.getType() == schemaType) {
            return true;
        }
        if (resolvedSchema.getOneOf() != null && !resolvedSchema.getOneOf().isEmpty()) {
            return resolvedSchema.getOneOf().stream().allMatch(oneOfSchema -> isSchemaOfType(api, oneOfSchema, schemaType));
        }
        if (resolvedSchema.getAnyOf() != null && !resolvedSchema.getAnyOf().isEmpty()) {
            return resolvedSchema.getAnyOf().stream().allMatch(anyOfSchema -> isSchemaOfType(api, anyOfSchema, schemaType));
        }
        if (resolvedSchema.getAllOf() != null && !resolvedSchema.getAllOf().isEmpty()) {
            return resolvedSchema.getAllOf().stream().anyMatch(allOfSchema -> isSchemaOfType(api, allOfSchema, schemaType));
        }
        return false;
    }

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

    public static Set<SchemaDefinitionDeprecated> getSchemas(OpenAPI api) {
        Set<SchemaDefinitionDeprecated> schemas = new HashSet<>();
        Components components = api.getComponents();
        if (components != null) {
            // Gets schemas from top-level components
            if (components.getSchemas() != null) {
                components.getSchemas().forEach((schemaName, schema) -> schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.COMPONENT_SCHEMAS, schemaName, schema)));
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
        parameters.forEach(parameter -> {
            if (parameter.getSchema() != null) {
                schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.PARAMETER, parameter.getName(), parameter.getSchema()));
            }
        });
        // Get schemas from operations
        getOperations(api).forEach(operation -> schemas.addAll(getSchemasFromOperation(operation)));

        Set<SchemaDefinitionDeprecated> nestedSchemas = new HashSet<>();
        schemas.forEach(schema -> nestedSchemas.addAll(getNestedSchemas(schema)));
        schemas.addAll(nestedSchemas);

        return schemas.stream().filter(schemaDefinition -> schemaDefinition.getSchema().getRef() == null).collect(Collectors.toSet());
    }

    private static Set<SchemaDefinitionDeprecated> getCallbackSchemas(Components components) {
        Set<SchemaDefinitionDeprecated> schemas = new HashSet<>();
        if (components.getCallbacks() != null) {
            components.getCallbacks().forEach((callbackName, callback) -> callback.getPathItems().forEach((pathItemName, pathItem) -> pathItem.getOperations().forEach(((httpMethod, operation) -> schemas.addAll(getSchemasFromOperation(operation))))));
        }

        return schemas;
    }

    private static Set<SchemaDefinitionDeprecated> getRequestBodySchemas(Components components) {
        Set<SchemaDefinitionDeprecated> schemas = new HashSet<>();
        // Get top level requestBodies
        if (components.getRequestBodies() != null) {
            components.getRequestBodies().forEach((requestName, request) -> {
                if (request.getContent() != null && request.getContent().getMediaTypes() != null) {
                    request.getContent().getMediaTypes().forEach((mediaTypeName, mediaType) -> {
                        if (mediaType.getSchema() != null) {
                            schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.COMPONENT_REQUESTBODY, requestName + ":" + mediaTypeName, mediaType.getSchema()));
                        }
                    });
                }
            });
        }
        return schemas;
    }

    private static Set<SchemaDefinitionDeprecated> getHeaderSchemas(Components components) {
        Set<SchemaDefinitionDeprecated> schemas = new HashSet<>();
        // Get top level header schemas
        if (components.getHeaders() != null) {
            components.getHeaders().forEach((headerName, header) -> {
                if (header.getSchema() != null) {
                    schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.COMPONENT_HEADER, headerName, header.getSchema()));
                }
            });
        }
        return schemas;
    }

    private static Set<SchemaDefinitionDeprecated> getTopLevelParameterSchemas(Components components) {
        Set<SchemaDefinitionDeprecated> schemas = new HashSet<>();
        if (components.getParameters() != null) {
            components.getParameters().forEach((parameterName, parameter) -> {
                if (parameter.getSchema() != null) {
                    schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.PARAMETER, parameterName, parameter.getSchema()));
                }
            });
        }
        return schemas;
    }

    private static Set<SchemaDefinitionDeprecated> getResponseSchemas(Components components) {
        Set<SchemaDefinitionDeprecated> schemas = new HashSet<>();
        // Get top level responses
        if (components.getResponses() != null) {
            components.getResponses().forEach((responseName, response) -> {

                Content content = response.getContent();
                if (content != null && content.getMediaTypes() != null) {
                    content.getMediaTypes().forEach((mediaTypeName, mediaType) -> {
                        if (mediaType.getSchema() != null) {
                            schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.COMPONENT_RESPONSES, responseName + ":" + mediaTypeName, mediaType.getSchema()));
                        }
                    });
                }
                if (response.getHeaders() != null) {
                    response.getHeaders().forEach((headerName, header) -> {
                        if (header.getSchema() != null) {
                            schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.INLINE_HEADER, responseName + ":" + headerName, header.getSchema()));
                        }
                    });
                }
            });
        }
        return schemas;
    }

    private static Set<SchemaDefinitionDeprecated> getSchemasFromOperation(Operation operation) {
        Set<SchemaDefinitionDeprecated> schemas = new HashSet<>();
        // Get responses
        if (operation.getResponses() != null && operation.getResponses().getAPIResponses() != null) {
            operation.getResponses().getAPIResponses().forEach((responseName, response) -> {
                if (response.getContent() != null && response.getContent().getMediaTypes() != null) {
                    response.getContent().getMediaTypes().forEach((mediaTypeName, mediaType) -> {
                        if (mediaType.getSchema() != null) {
                            schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.INLINE_RESPONSES, operation.getOperationId() + ":" + responseName + ":" + mediaTypeName, mediaType.getSchema()));
                        }
                    });
                }
                if (response.getHeaders() != null) {
                    response.getHeaders().forEach((headerName, header) -> {
                        if (header.getSchema() != null) {
                            schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.INLINE_HEADER, operation.getOperationId() + ":" + responseName + ":" + headerName, header.getSchema()));
                        }
                    });
                }
            });
        }
        // Get requestbodies
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null && operation.getRequestBody().getContent().getMediaTypes() != null) {
            operation.getRequestBody().getContent().getMediaTypes().forEach((mediaTypeName, mediaType) -> {
                if (mediaType.getSchema() != null) {
                    schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.INLINE_REQUESTBODY, operation.getOperationId() + ":" + mediaTypeName, mediaType.getSchema()));
                }
            });
        }
        // Get parameters
        if (operation.getParameters() != null) {
            Set<Parameter> parameters = new HashSet<>(operation.getParameters());
            parameters.forEach(parameter -> {
                if (parameter.getSchema() != null) {
                    schemas.add(new SchemaDefinitionDeprecated(OpenApiDefinitionLocation.PARAMETER, parameter.getName(), parameter.getSchema()));
                }
            });
        }
        // Get callbacks
        if (operation.getCallbacks() != null) {
            operation.getCallbacks().forEach((callbackName, callback) -> callback.getPathItems().forEach((pathItemName, pathItem) -> pathItem.getOperations().forEach((httpMethod, callbackOperation) -> schemas.addAll(getSchemasFromOperation(callbackOperation)))));
        }

        return schemas;
    }

    private static Set<SchemaDefinitionDeprecated> getNestedSchemas(SchemaDefinitionDeprecated parentSchemaDefinition) {
        Set<SchemaDefinitionDeprecated> schemas = new HashSet<>();
        var parentSchema = parentSchemaDefinition.getSchema();
        if (parentSchema.getProperties() != null) {
            parentSchema.getProperties().forEach((schemaName, schema) -> {
                var schemaOfProperty = new SchemaDefinitionDeprecated(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":" + schemaName, schema);
                schemas.add(schemaOfProperty);
                schemas.addAll(getNestedSchemas(schemaOfProperty));
            });
        }
        if (parentSchema.getItems() != null) {
            var schemaOfProperty = new SchemaDefinitionDeprecated(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":items", parentSchema.getItems());
            schemas.add(schemaOfProperty);
            schemas.addAll(getNestedSchemas(schemaOfProperty));
        }
        if (parentSchema.getAllOf() != null) {
            parentSchema.getAllOf().forEach(schema -> {
                var schemaOfProperty = new SchemaDefinitionDeprecated(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":allOf", schema);
                schemas.add(schemaOfProperty);
                schemas.addAll(getNestedSchemas(schemaOfProperty));
            });
        }
        if (parentSchema.getAnyOf() != null) {
            parentSchema.getAnyOf().forEach(schema -> {
                var schemaOfProperty = new SchemaDefinitionDeprecated(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":anyOf", schema);
                schemas.add(schemaOfProperty);
                schemas.addAll(getNestedSchemas(schemaOfProperty));
            });
        }
        if (parentSchema.getOneOf() != null) {
            parentSchema.getOneOf().forEach(schema -> {
                var schemaOfProperty = new SchemaDefinitionDeprecated(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":oneOf", schema);
                schemas.add(schemaOfProperty);
                schemas.addAll(getNestedSchemas(schemaOfProperty));
            });
        }
        if (parentSchema.getAdditionalPropertiesSchema() != null) {
            var schemaOfProperty = new SchemaDefinitionDeprecated(parentSchemaDefinition.getParentDefinitionLocation(), parentSchemaDefinition.parentName + ":additionalProperties", parentSchema.getAdditionalPropertiesSchema());
            schemas.add(schemaOfProperty);
            schemas.addAll(getNestedSchemas(schemaOfProperty));
        }
        return schemas;
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
//        return string.matches("^[a-z0-9]+([A-Z]?[a-z0-9]+)*$");
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