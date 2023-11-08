package be.belgium.gcloud.rest.styleguide.validation.core.parser;

import be.belgium.gcloud.rest.styleguide.validation.LineRangePath;
import be.belgium.gcloud.rest.styleguide.validation.core.Line;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.models.*;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.openapitools.empoa.swagger.core.internal.SwAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class Parser {

    private File openApiFile;

    @Getter
    @Setter
    public static class ParserResult {
        private Set<PathDefinition> pathDefinitions = new HashSet<>();
        private Set<MediaTypeDefinition> mediaTypes = new HashSet<>();
        private Set<RequestBodyDefinition> requestBodies = new HashSet<>();
        private Set<ResponseDefinition> responses = new HashSet<>();
        private Set<OperationDefinition> operations = new HashSet<>();
        private Set<SchemaDefinition> schemas = new HashSet<>();
        private Set<ParameterDefinition> parameters = new HashSet<>();
        private Set<ResponseHeaderDefinition> headers = new HashSet<>();
        private Set<ServerDefinition> servers = new HashSet<>();
        private Set<OpenApiDefinition<? extends Constructible>> allDefinitions = new HashSet<>();

        private OpenAPI openAPI;
        private List<LineRangePath> paths;
        public int oasVersion;
        public File openApiFile;
        private Map<String, SourceDefinition> src;

        private void assembleAllDefinitions() {
            allDefinitions.addAll(pathDefinitions);
            allDefinitions.addAll(requestBodies);
            allDefinitions.addAll(responses);
            allDefinitions.addAll(operations);
            allDefinitions.addAll(mediaTypes);
            allDefinitions.addAll(schemas);
            allDefinitions.addAll(parameters);
            allDefinitions.addAll(headers);
            allDefinitions.addAll(servers);
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

        /**
         * Return true if at least one element that {LineRangePath.path IN paths and LineRangePath.start >= lineNumber < LineRangePath.end}
         *
         * @param paths
         * @param lineNumber
         * @return
         */
        public boolean isInPathList(List<String> paths, int lineNumber) {
            return this.getPaths().stream().anyMatch(lineRangePath -> paths.contains(lineRangePath.getPath()) && lineRangePath.inRange(lineNumber));
        }

    }

    public ParserResult parse(OpenApiViolationAggregator openApiViolationAggregator) {
        try {
            ParserResult result = new ParserResult();
            result.openApiFile = openApiFile;
            result.src = readOpenApiFiles(openApiFile);
            for (SourceDefinition sourceDefinition : result.src.values()) {
                parseComponents(sourceDefinition, result);
                if (sourceDefinition.getFile() == result.openApiFile) {
                    result.openAPI = sourceDefinition.getOpenApi();
                    result.setOasVersion(getOasVersion(sourceDefinition.getSrc()));
                    parseServers(result);
                    parsePaths(sourceDefinition, result);
                }
            }
            result.assembleAllDefinitions();
            buildAllPathWithLineRange(result);
            return result;
        } catch (IOException e) {
            openApiViolationAggregator.addViolation(e.getClass().getSimpleName(), e.getLocalizedMessage(), new Line(openApiFile.getName(), 0));
            return null;
        }
    }

    /**
     * For all openAPI.path.key build a LineRangePath whith the start and end line of the path.
     */
    public void buildAllPathWithLineRange(ParserResult result) {
        var paths = new ArrayList<LineRangePath>();

        if (result.getPathDefinitions().isEmpty()) {
            result.paths = Collections.emptyList();
        } else {
            result.pathDefinitions.forEach(p -> paths.add(p.getLineRangePath()));
            Collections.sort(paths);

            result.paths = paths;
        }
    }

    public static OpenAPI buildOpenApiSpecification(File file) throws IOException {
        var openApiParser = new OpenAPIParser();
        var parseOptions = new ParseOptions();
        parseOptions.setResolve(false);

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

    private static int getOasVersion(String jsonString) {
        if (jsonString.contains("openapi:") || jsonString.contains("\"openapi\":")) {
            return 3;
        } else {
            return 2;
        }
    }

    private void parseServers(ParserResult result) {
        List<Server> serverModels = result.openAPI.getServers();
        for (Server server : serverModels) {
            int index = serverModels.indexOf(server);
            ServerDefinition def = new ServerDefinition(server, server.getUrl(), openApiFile, index, result);
            result.servers.add(def);
        }
    }

    private void parsePaths(SourceDefinition sourceDefinition, ParserResult result) {
        Paths paths = sourceDefinition.getOpenApi().getPaths();
        var openApiFile = sourceDefinition.getFile();
        if (paths == null) {
            return;
        }
        Map<String, PathItem> pathItems = paths.getPathItems();
        pathItems.forEach((path, pathitem) -> {
            PathDefinition pathDef = new PathDefinition(pathitem, path, openApiFile, result);
            result.pathDefinitions.add(pathDef);
            if (pathitem.getOperations() != null) {
                pathitem.getOperations().forEach((method, operation) -> {
                    var operationDef = new OperationDefinition(operation, pathDef, method);
                    result.operations.add(operationDef);
                    parseOperation(operationDef, result);
                });
            }
            if (pathitem.getParameters() != null) {
                pathitem.getParameters().forEach(parameter -> {
                    int index = pathitem.getParameters().indexOf(parameter);
                    var paramDef = new ParameterDefinition(parameter, pathDef, parameter.getName(), index);
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
                    int index = parameters.indexOf(parameter);
                    var parameterDefinition = new ParameterDefinition(parameter, operationDef, parameter.getName(), index);
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

    public void parseComponents(SourceDefinition sourceDefinition, ParserResult result) {
        Components components = sourceDefinition.getOpenApi().getComponents();
        var openApiFile = sourceDefinition.getFile();
        if (components == null) {
            return;
        }
        Map<String, APIResponse> responses = components.getResponses();
        if (responses != null) {
            responses.forEach((name, response) -> {
                var responseDef = new ResponseDefinition(response, name, openApiFile, result);
                result.responses.add(responseDef);
                parseResponse(responseDef, result);
            });
        }

        Map<String, RequestBody> requestBodies = components.getRequestBodies();
        if (requestBodies != null) {
            requestBodies.forEach((name, requestBody) -> {
                var requestBodyDef = new RequestBodyDefinition(requestBody, name, openApiFile, result);
                result.requestBodies.add(requestBodyDef);
                parseRequestBody(requestBodyDef, result);
            });
        }

        Map<String, Schema> schemas = components.getSchemas();
        if (schemas != null) {
            schemas.forEach((name, schema) -> {
                var schemaDef = new SchemaDefinition(schema, name, openApiFile, result);
                result.schemas.add(schemaDef);
                parseSchema(schemaDef, result);
            });
        }

        Map<String, Header> headers = components.getHeaders();
        if (headers != null) {
            headers.forEach((name, header) -> {
                var headerDef = new ResponseHeaderDefinition(header, name, openApiFile, result);
                result.headers.add(headerDef);
                parseHeaders(headerDef, result);
            });
        }
        Map<String, Parameter> parameters = components.getParameters();
        if (parameters != null) {
            parameters.forEach((name, parameter) -> {
                var parameterDef = new ParameterDefinition(parameter, name, openApiFile, result);
                result.parameters.add(parameterDef);
                parseParameter(parameterDef, result);
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
        constructNestedSchema(parentSchema.getAllOf(), JsonPointer.relative("allOf"), schemaDefinition, result);
        constructNestedSchema(parentSchema.getOneOf(), JsonPointer.relative("oneOf"), schemaDefinition, result);
        constructNestedSchema(parentSchema.getAnyOf(), JsonPointer.relative("anyOf"), schemaDefinition, result);
        constructNestedSchema(parentSchema.getAdditionalPropertiesSchema(), JsonPointer.relative("additionalProperties"), schemaDefinition, result);
        constructNestedSchema(parentSchema.getItems(), JsonPointer.relative("items"), schemaDefinition, result);
        if (parentSchema.getProperties() != null) {
            parentSchema.getProperties().forEach((propertyName, propertyObject) ->
                    constructNestedSchema(propertyObject, JsonPointer.relative("properties").add(propertyName), schemaDefinition, result));
        }
    }

    private void constructNestedSchema(List<Schema> schemas, JsonPointer relativePointer, SchemaDefinition parentSchema, ParserResult result) {
        if (schemas != null) {
            int index = 0;
            while (index < schemas.size()) {
                constructNestedSchema(schemas.get(index), relativePointer.add(index), parentSchema, result);
                index++;
            }
        }
    }

    private void constructNestedSchema(Schema schema, JsonPointer relativePointer, SchemaDefinition parentSchema, ParserResult result) {
        if (schema != null && schema.getRef() == null) {
            var schemaDef = new SchemaDefinition(schema, parentSchema, schema.getTitle(), relativePointer);
            result.schemas.add(schemaDef);
            parseSchema(schemaDef, result);
        }
    }

    private static List<String> getLines(File file) throws IOException {
        var lines = Files.readAllLines(file.toPath());

        // lines > 1 then is a yaml or a pretty json file
        if (lines.size() > 1) return lines;

        // else is a ugly json file
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var pretty = gson.toJson(JsonParser.parseString(lines.get(0)));
        return pretty.lines().collect(Collectors.toList());
    }

    private Set<File> getReferencedFiles(File file) throws IOException {
        Set<File> refFiles = new HashSet<>();
        resolveReferences(file, refFiles);
        return refFiles;
    }

    private void resolveReferences(File file, Set<File> files) throws IOException {
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

    private Set<String> getExternalReferencesFromFile(File file) throws IOException {
        Set<String> references = new HashSet<>();
        ObjectMapper mapper;

        if (SourceDefinition.checkIsYaml(file.getName())) {
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
            arrayField.forEach(field -> findRefFields(field, refs));
        }
    }

    private Map<String, SourceDefinition> readOpenApiFiles(File file) throws IOException {
        Map<String, SourceDefinition> openApiFiles = new HashMap<>();
        SourceDefinition mainFile = new SourceDefinition(file, buildOpenApiSpecification(file));
        openApiFiles.put(file.getAbsolutePath(), mainFile);
        Set<File> refFiles = getReferencedFiles(file);
        for (File refFile : refFiles) {
            SourceDefinition refFileDef = new SourceDefinition(refFile, buildOpenApiSpecification(refFile));
            openApiFiles.put(refFile.getAbsolutePath(), refFileDef);
        }
        return openApiFiles;
    }


}
