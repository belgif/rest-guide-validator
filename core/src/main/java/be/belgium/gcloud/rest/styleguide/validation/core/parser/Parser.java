package be.belgium.gcloud.rest.styleguide.validation.core.parser;

import be.belgium.gcloud.rest.styleguide.validation.LineRangePath;
import be.belgium.gcloud.rest.styleguide.validation.core.Line;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.*;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.openapitools.empoa.swagger.core.internal.SwAdapter;
import org.openapitools.empoa.swagger.core.internal.models.examples.SwExample;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
        private Set<ExampleDefinition> examples = new HashSet<>();
        private Set<SecuritySchemeDefinition> securitySchemes = new HashSet<>();
        private Set<LinkDefinition> links = new HashSet<>();
        private Set<SecurityRequirementDefinition> securityRequirements = new HashSet<>();
        private Set<OpenApiDefinition<? extends Constructible>> allDefinitions = new HashSet<>();

        private OpenAPI openAPI;
        private List<LineRangePath> paths;
        public int oasVersion;
        public File openApiFile;
        private Map<String, SourceDefinition> src;
        private boolean isParsingValid = true;

        private List<String> parsingViolation = new ArrayList<>();

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
            allDefinitions.addAll(examples);
            allDefinitions.addAll(securitySchemes);
            allDefinitions.addAll(links);
            allDefinitions.addAll(securityRequirements);
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
                        var violation = "[Internal error] Could not find match of " + ref;
                        log.error(violation);
                        parsingViolation.add(violation);
                        return null;
                    }
                }
            }

            // no ref
            var defMatch = allDefinitions.stream().filter(def -> def.getModel() == model).findAny();
            if (defMatch.isPresent()) {
                return (OpenApiDefinition<T>) defMatch.get();
            } else {
                var violation = "[Internal error] Could not find match of " + model;
                log.error(violation);
                parsingViolation.add(violation);
                return null;
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
                parseGlobalSecurityRequirements(sourceDefinition, result);
                if (sourceDefinition.getFile() == result.openApiFile) {
                    result.openAPI = sourceDefinition.getOpenApi();
                    result.setOasVersion(getOasVersion(sourceDefinition));
                    parseServers(result);
                    parsePaths(sourceDefinition, result);
                }
            }
            verifySecurityRequirements(result);
            result.assembleAllDefinitions();
            buildAllPathWithLineRange(result);
            if( ! result.getParsingViolation().isEmpty()) {
                result.getParsingViolation().forEach(parsingViolation ->
                        openApiViolationAggregator.addViolation("PARSING", parsingViolation, new Line(openApiFile.getName(), 0)));

            }
            if (result.isParsingValid()) {
                return result;
            }
            throw new RuntimeException("Parsing openapi definition failed. Please review logs.");
        } catch (IOException e) {
            openApiViolationAggregator.addViolation(e.getClass().getSimpleName(), e.getLocalizedMessage(), new Line(openApiFile.getName(), 0));
            return null;
        }
    }

    public void verifySecurityRequirements(ParserResult result) {
        Set<String> allowedRequirements = result.getSecuritySchemes().stream().map(OpenApiDefinition::getIdentifier).collect(Collectors.toSet());
        result.securityRequirements.forEach(securityRequirement -> {
            for (String securityScheme : securityRequirement.getModel().getSchemes().keySet()) {
                if (!allowedRequirements.contains(securityScheme)) {
                    log.error("OpenApi parsing error: SecurityScheme: <<{}>> is used in <<{}>>, but is not defined", securityScheme, securityRequirement.getJsonPointer().toPrettyString());
                    result.setParsingValid(false);
                }
            }
        });
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

    private static int getOasVersion(SourceDefinition sourceDefinition) {
        ObjectMapper mapper;

        if (sourceDefinition.isYaml()) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else {
            mapper = new ObjectMapper();
        }
        try {
            JsonNode jsonNode = mapper.readTree(sourceDefinition.getFile());
            if (jsonNode.has("openapi")) {
                return 3;
            } else {
                return 2;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error finding oas version for: " + sourceDefinition.getFile().getName(), e);
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
        if (param != null) {
            if (param.getSchema() != null) {
                var schemaDefinition = new SchemaDefinition(param.getSchema(), parameterDefinition);
                result.schemas.add(schemaDefinition);
                parseSchema(schemaDefinition, result);
            }
            if (param.getContent() != null && param.getContent().getMediaTypes() != null) {
                Map<String, MediaType> mediaTypes = param.getContent().getMediaTypes();
                mediaTypes.forEach((mediaType, mediaTypeObject) -> {
                    var mediaTypeDef = new MediaTypeDefinition(mediaTypeObject, parameterDefinition, mediaType);
                    result.mediaTypes.add(mediaTypeDef);
                    parseMediaType(mediaTypeDef, result);
                });
            }
            constructExamples(parameterDefinition, parameterDefinition.getModel().getExample(), parameterDefinition.getModel().getExamples(), result);
        }
    }

    private void parseOperation(OperationDefinition operationDef, ParserResult result) {
        RequestBody requestBody = operationDef.getModel().getRequestBody();
        if (requestBody != null) {
            var requestBodyDefinition = new RequestBodyDefinition(requestBody, operationDef);
            result.requestBodies.add(requestBodyDefinition);
            parseRequestBody(requestBodyDefinition, result);
        }

        List<Parameter> parameters = operationDef.getModel().getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                int index = parameters.indexOf(parameter);
                var parameterDefinition = new ParameterDefinition(parameter, operationDef, parameter.getName(), index);
                result.parameters.add(parameterDefinition);
                parseParameter(parameterDefinition, result);
            }
        }

        APIResponses apiResponses = operationDef.getModel().getResponses();
        if (apiResponses != null) {
            var responses = apiResponses.getAPIResponses();
            responses.forEach((statusCode, responseObject) -> {
                var responseDef = new ResponseDefinition(responseObject, operationDef, statusCode);
                result.responses.add(responseDef);
                parseResponse(responseDef, result);
            });
        }

        List<SecurityRequirement> securityRequirements = operationDef.getModel().getSecurity();
        if (securityRequirements != null) {
            securityRequirements.forEach(securityRequirement -> result.securityRequirements.add(new SecurityRequirementDefinition(securityRequirement, operationDef, securityRequirements.indexOf(securityRequirement))));
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
        Map<String, Example> examples = components.getExamples();
        if (examples != null) {
            examples.forEach((name, example) -> {
                var exampleDef = new ExampleDefinition(example, name, openApiFile, result);
                result.examples.add(exampleDef);
            });
        }
        Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();
        if (securitySchemes != null) {
            securitySchemes.forEach((name, scheme) -> result.securitySchemes.add(new SecuritySchemeDefinition(scheme, name, openApiFile, result)));
        }
        Map<String, Link> links = components.getLinks();
        if (links != null) {
            links.forEach((name, link) -> result.links.add(new LinkDefinition(link, name, openApiFile, result)));
        }
    }

    public void parseGlobalSecurityRequirements(SourceDefinition sourceDefinition, ParserResult result) {
        List<SecurityRequirement> securityRequirements = sourceDefinition.getOpenApi().getSecurity();
        if (securityRequirements != null) {
            securityRequirements.forEach(securityRequirement -> result.securityRequirements.add(new SecurityRequirementDefinition(securityRequirement, securityRequirements.indexOf(securityRequirement), sourceDefinition.getFile(), result)));
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
                var headerDef = new ResponseHeaderDefinition(header, responseDef, name);
                result.headers.add(headerDef);
                parseHeaders(headerDef, result);
            });
        }
        var links = responseDef.getModel().getLinks();
        if (links != null) {
            links.forEach((name, link) -> result.links.add(new LinkDefinition(link, responseDef, name)));
        }
    }

    public void parseHeaders(ResponseHeaderDefinition responseHeaderDefinition, ParserResult result) {
        var schema = responseHeaderDefinition.getModel().getSchema();
        if (schema != null) {
            var schemaDef = new SchemaDefinition(schema, responseHeaderDefinition);
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
        constructExamples(responseHeaderDefinition, responseHeaderDefinition.getModel().getExample(), responseHeaderDefinition.getModel().getExamples(), result);
    }

    public void parseMediaType(MediaTypeDefinition mediaTypeDefinition, ParserResult result) {
        var schema = mediaTypeDefinition.getModel().getSchema();
        if (schema != null) {
            var schemaDef = new SchemaDefinition(schema, mediaTypeDefinition);
            result.schemas.add(schemaDef);
            parseSchema(schemaDef, result);
        }
        constructExamples(mediaTypeDefinition, mediaTypeDefinition.getModel().getExample(), mediaTypeDefinition.getModel().getExamples(), result);
    }

    public void parseSchema(SchemaDefinition schemaDefinition, ParserResult result) {
        var parentSchema = schemaDefinition.getModel();
        constructExamples(schemaDefinition, parentSchema.getExample(), null, result);
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
        if (schema != null) {
            var schemaDef = new SchemaDefinition(schema, parentSchema, relativePointer);
            result.schemas.add(schemaDef);
            parseSchema(schemaDef, result);
        }
    }

    private void constructExamples(OpenApiDefinition<?> parent, Object example, Map<String, Example> examples, ParserResult result) {
        if (example != null) {
            Example exampleObject = new SwExample();
            exampleObject.setValue(example);
            result.examples.add(new ExampleDefinition(exampleObject, parent));
        }
        if (examples != null) {
            examples.forEach((name, value) -> result.examples.add(new ExampleDefinition(value, parent, name)));
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

    private Set<File> getReferencedFiles(File file) {
        Set<File> refFiles = new HashSet<>();
        resolveReferences(file, refFiles);
        return refFiles;
    }

    private void resolveReferences(File file, Set<File> files) {
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

    private Set<String> getExternalReferencesFromFile(File file) {
        Set<String> references = new HashSet<>();
        ObjectMapper mapper;

        if (SourceDefinition.checkIsYaml(file.getName())) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else {
            mapper = new ObjectMapper();
        }

        try {
            JsonNode jsonNode = mapper.readTree(file);
            findRefFields(jsonNode, references);
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                int location = ((JsonProcessingException) e).getLocation().getLineNr();
                throw new RuntimeException("Error parsing external references of: " + file.getName() + "; Line: " + location, e);
            } else {
                throw new RuntimeException("Error parsing external references of: " + file.getName(), e);
            }
        }
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
