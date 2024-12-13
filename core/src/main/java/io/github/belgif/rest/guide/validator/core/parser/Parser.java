package io.github.belgif.rest.guide.validator.core.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.github.belgif.rest.guide.validator.LineRangePath;
import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.constant.ExpectedReferencePathConstants;
import io.github.belgif.rest.guide.validator.core.model.*;
import io.github.belgif.rest.guide.validator.core.util.ExampleMapper;
import io.github.belgif.rest.guide.validator.core.util.SchemaValidator;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
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
        private Set<PathsDefinition> pathsDefinitions = new HashSet<>();
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
        private final Map<Constructible, OpenApiDefinition<?>> modelDefinitionMap = new HashMap<>();

        private void assembleAllDefinitions() {
            allDefinitions.addAll(pathsDefinitions);
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
            allDefinitions.forEach(def -> modelDefinitionMap.put(def.getModel(), def));
        }

        @SuppressWarnings("unchecked")
        public <T extends Constructible> OpenApiDefinition<T> resolve(T model) {
            if (model instanceof Reference) {
                var ref = ((Reference<?>) model).getRef();
                if (ref != null) {
                    File refOpenApiFile = findOpenApiFileForRef(ref, model);
                    JsonPointer pointer = buildJsonPointerFromRef(ref, refOpenApiFile);
                    return (OpenApiDefinition<T>) allDefinitions.stream().filter(def ->
                                    def.getJsonPointer().equals(pointer) && def.getOpenApiFile().equals(refOpenApiFile))
                            .findFirst().orElseThrow(() -> new RuntimeException("[Internal error] Could not find match of " + ref));
                }
            }

            // no ref
            if (modelDefinitionMap.containsKey(model)) {
                return (OpenApiDefinition<T>) modelDefinitionMap.get(model);
            } else {
                throw new RuntimeException("[Internal error] Could not find match of " + model.toString());
            }
        }

        private JsonPointer buildJsonPointerFromRef(String ref, File refOpenApiFile) {
            ref = !ref.startsWith("#") ? ref.split("#")[1] : ref.substring(1);
            if (!ref.startsWith("/components") && this.oasVersion == 2) {
                // SwaggerParser usually changes references to OAS3 standard. But not for external references.
                // So we have to try both options.
                String[] refParts = ref.substring(1).split("/");
                String refGroup = "/" + refParts[0];
                String objectName = refParts[refParts.length - 1];
                if (refGroup.equals("/parameters")) {
                    // Special case because this can be both a parameter and requestBody in oas3.
                    var object = allDefinitions.stream().filter(definition -> definition instanceof ParameterDefinition || definition instanceof RequestBodyDefinition)
                            .filter(definition -> definition.getOpenApiFile().equals(refOpenApiFile) && definition.getIdentifier() != null && definition.getIdentifier().equals(objectName)).findFirst();
                    if (object.isPresent()) {
                        return object.get().getJsonPointer();
                    }
                }
                for (Map.Entry<Class<?>, String> entry : ExpectedReferencePathConstants.OAS_2_LOCATIONS.entrySet()) {
                    if (entry.getValue().equals(refGroup)) {
                        ref = ExpectedReferencePathConstants.OAS_3_LOCATIONS.get(entry.getKey()) + "/" + objectName;
                        break;
                    }
                }
            }
            return new JsonPointer(ref);
        }

        private File findOpenApiFileForRef(String ref, Constructible model) {
            File modelBaseFile = modelDefinitionMap.get(model).getOpenApiFile();
            String fileRef = ref.split("#")[0];
            if (fileRef.isEmpty()) {
                return modelBaseFile;
            }
            Path basePath = java.nio.file.Paths.get(modelBaseFile.getParent());
            return resolveRelativeFile(fileRef, basePath);
        }

        /**
         * @param paths
         * @param lineNumber
         * @return true if at least one element that {LineRangePath.path IN paths and LineRangePath.start >= lineNumber &lt; LineRangePath.end}
         */
        public boolean isInPathList(List<String> paths, int lineNumber) {
            return this.getPaths().stream().anyMatch(lineRangePath -> paths.contains(lineRangePath.getPath()) && lineRangePath.inRange(lineNumber));
        }

    }

    public ParserResult parse(ViolationReport violationReport) {
        try {
            var result = new ParserResult();
            result.openApiFile = openApiFile;
            result.src = readOpenApiFiles(openApiFile);
            result.setOasVersion(
                    getOasVersion(result.src.get(openApiFile.getAbsolutePath())));
            for (SourceDefinition sourceDefinition : result.src.values()) {
                parsePaths(sourceDefinition, result);
                parseComponents(sourceDefinition, result);
                parseGlobalSecurityRequirements(sourceDefinition, result);
                if (sourceDefinition.getFile() == result.openApiFile) {
                    result.openAPI = sourceDefinition.getOpenApi();
                    if (!sourceDefinition.isPathsUsedAsRefsOnly()) {
                        parseServers(result);
                    }
                }
            }
            verifySecurityRequirements(result);
            result.assembleAllDefinitions();
            buildAllPathWithLineRange(result);
            if (result.isParsingValid()) {
                return result;
            }
            // Double log because: The exception message is a bit separated from the parsing errors in the output, and only added to the end of some long output line.
            log.error("Input file is not a valid OpenAPI document. Compliance to the REST style guidelines could not be verified.");
            throw new RuntimeException("Input file is not a valid OpenAPI document. Compliance to the REST style guidelines could not be verified.");
        } catch (IOException e) {
            violationReport.addViolation(e.getClass().getSimpleName(), e.getLocalizedMessage(), new Line(openApiFile.getName(), 0), "#");
            return null;
        }
    }

    public void verifySecurityRequirements(ParserResult result) {
        var allowedRequirements = result.getSecuritySchemes().stream().map(OpenApiDefinition::getIdentifier).collect(Collectors.toSet());
        result.securityRequirements.forEach(securityRequirement -> {
            for (String securityScheme : securityRequirement.getModel().getSchemes().keySet()) {
                if (!allowedRequirements.contains(securityScheme)) {
                    log.error(securityRequirement.getFullyQualifiedPointer() + ": Security Scheme <<{}>> is not defined", securityScheme);
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
            String version;
            var openApiLine = getLines(file).stream().filter(line -> line.trim().startsWith("openapi: ")).findFirst();
            if (openApiLine.isPresent()) {
                version = openApiLine.get().substring(9);
            } else {
                throw new RuntimeException("Input file is not an OpenApi or Swagger file, or version number could not be found. <<" + file.getAbsolutePath() + ">>");
            }
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
            var jsonNode = mapper.readTree(sourceDefinition.getFile());
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
        var serverModels = result.openAPI.getServers();
        for (Server server : serverModels) {
            int index = serverModels.indexOf(server);
            var def = new ServerDefinition(server, server.getUrl(), openApiFile, index, result);
            result.servers.add(def);
        }
    }

    private void parsePaths(SourceDefinition sourceDefinition, ParserResult result) {
        var paths = sourceDefinition.getOpenApi().getPaths();
        var openApiFile = sourceDefinition.getFile();
        if (paths == null) {
            return;
        }
        PathsDefinition pathsDefinition = new PathsDefinition(paths, openApiFile, result);
        result.pathsDefinitions.add(pathsDefinition);
        var pathItems = paths.getPathItems();
        pathItems.forEach((path, pathitem) -> {
            PathDefinition pathDef = new PathDefinition(pathitem, pathsDefinition, path);
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
        var param = parameterDefinition.getModel();
        if (param != null) {
            if (param.getSchema() != null) {
                var schemaDefinition = new SchemaDefinition(param.getSchema(), parameterDefinition);
                result.schemas.add(schemaDefinition);
                parseSchema(schemaDefinition, result);
            }
            if (param.getContent() != null && param.getContent().getMediaTypes() != null) {
                var mediaTypes = param.getContent().getMediaTypes();
                mediaTypes.forEach((mediaType, mediaTypeObject) -> {
                    var mediaTypeDef = new MediaTypeDefinition(mediaTypeObject, parameterDefinition, mediaType);
                    result.mediaTypes.add(mediaTypeDef);
                    parseMediaType(mediaTypeDef, result);
                });
            }
            constructExamples(parameterDefinition, result);
        }
    }

    private void parseOperation(OperationDefinition operationDef, ParserResult result) {
        var requestBody = operationDef.getModel().getRequestBody();
        if (requestBody != null) {
            var requestBodyDefinition = new RequestBodyDefinition(requestBody, operationDef);
            result.requestBodies.add(requestBodyDefinition);
            parseRequestBody(requestBodyDefinition, result);
        }

        var parameters = operationDef.getModel().getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                int index = parameters.indexOf(parameter);
                var parameterDefinition = new ParameterDefinition(parameter, operationDef, parameter.getName(), index);
                result.parameters.add(parameterDefinition);
                parseParameter(parameterDefinition, result);
            }
        }

        var apiResponses = operationDef.getModel().getResponses();
        if (apiResponses != null) {
            var responses = apiResponses.getAPIResponses();
            responses.forEach((statusCode, responseObject) -> {
                var responseDef = new ResponseDefinition(responseObject, operationDef, statusCode);
                result.responses.add(responseDef);
                parseResponse(responseDef, result);
            });
        }

        var securityRequirements = operationDef.getModel().getSecurity();
        if (securityRequirements != null) {
            securityRequirements.forEach(securityRequirement -> result.securityRequirements.add(new SecurityRequirementDefinition(securityRequirement, operationDef, securityRequirements.indexOf(securityRequirement))));
        }
    }

    public void parseComponents(SourceDefinition sourceDefinition, ParserResult result) {
        var components = sourceDefinition.getOpenApi().getComponents();
        var openApiFile = sourceDefinition.getFile();
        if (components == null) {
            return;
        }
        var responses = components.getResponses();
        if (responses != null) {
            responses.forEach((name, response) -> {
                var responseDef = new ResponseDefinition(response, name, openApiFile, result);
                result.responses.add(responseDef);
                parseResponse(responseDef, result);
            });
        }

        var requestBodies = components.getRequestBodies();
        if (requestBodies != null) {
            requestBodies.forEach((name, requestBody) -> {
                var requestBodyDef = new RequestBodyDefinition(requestBody, name, openApiFile, result);
                result.requestBodies.add(requestBodyDef);
                parseRequestBody(requestBodyDef, result);
            });
        }

        var schemas = components.getSchemas();
        if (schemas != null) {
            schemas.forEach((name, schema) -> {
                var schemaDef = new SchemaDefinition(schema, name, openApiFile, result);
                result.schemas.add(schemaDef);
                parseSchema(schemaDef, result);
            });
        }

        var headers = components.getHeaders();
        if (headers != null) {
            headers.forEach((name, header) -> {
                var headerDef = new ResponseHeaderDefinition(header, name, openApiFile, result);
                result.headers.add(headerDef);
                parseHeaders(headerDef, result);
            });
        }
        var parameters = components.getParameters();
        if (parameters != null) {
            parameters.forEach((name, parameter) -> {
                var parameterDef = new ParameterDefinition(parameter, name, openApiFile, result);
                result.parameters.add(parameterDef);
                parseParameter(parameterDef, result);
            });
        }
        var examples = components.getExamples();
        if (examples != null) {
            examples.forEach((name, example) -> {
                var exampleDef = new ExampleDefinition(example, name, openApiFile, result);
                result.examples.add(exampleDef);
            });
        }
        var securitySchemes = components.getSecuritySchemes();
        if (securitySchemes != null) {
            securitySchemes.forEach((name, scheme) -> result.securitySchemes.add(new SecuritySchemeDefinition(scheme, name, openApiFile, result)));
        }
        var links = components.getLinks();
        if (links != null) {
            links.forEach((name, link) -> result.links.add(new LinkDefinition(link, name, openApiFile, result)));
        }
    }

    public void parseGlobalSecurityRequirements(SourceDefinition sourceDefinition, ParserResult result) {
        var securityRequirements = sourceDefinition.getOpenApi().getSecurity();
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
        constructExamples(responseHeaderDefinition, result);
    }

    public void parseMediaType(MediaTypeDefinition mediaTypeDefinition, ParserResult result) {
        var schema = mediaTypeDefinition.getModel().getSchema();
        if (schema != null) {
            var schemaDef = new SchemaDefinition(schema, mediaTypeDefinition);
            result.schemas.add(schemaDef);
            parseSchema(schemaDef, result);
        }
        constructExamples(mediaTypeDefinition, result);
    }

    public void parseSchema(SchemaDefinition schemaDefinition, ParserResult result) {
        var parentSchema = schemaDefinition.getModel();
        constructExamples(schemaDefinition, result);
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

    /*
    Custom implementation because the standard swagger-parser is too strict in parsing the examples:
        When example cannot be cast to a java type linked to the schema,
        it sets the example property to null, and ignores all errors.
    This custom implementation retrieves the example as JsonNode from the contract file itself.
     */
    private void constructExamples(OpenApiDefinition<?> definition, ParserResult result) {
        try {
            var schemaNode = SchemaValidator.getSchemaNode(definition);
            if (schemaNode.has("example")) {
                var exampleValue = schemaNode.get("example");
                var exampleObject = new SwExample();
                exampleObject.setValue(exampleValue);
                result.examples.add(new ExampleDefinition(exampleObject, definition));
            } else if (schemaNode.has("examples")) {
                var examplesNode = schemaNode.get("examples");
                var iterator = examplesNode.fieldNames();
                while (iterator.hasNext()) {
                    var fieldName = iterator.next();
                    var exampleObject = ExampleMapper.mapToExampleObject(examplesNode.get(fieldName));
                    result.examples.add(new ExampleDefinition(exampleObject, definition, fieldName));
                }
            }
        } catch (JsonPointerOas2Exception e) {
            log.debug("Example validation in this location isn't supported for OAS2: {}", e.getMessage());
        } catch (Exception e) {
            if (result.getOasVersion() == 2) {
                /*
                It seems impossible to predict all JsonPointer translation mistakes from OAS3 to OAS2.
                To not let builds fail due to shortcomings of the validator, these parsing exceptions are ignored for OAS2 contracts.
                 */
                log.warn("Unable to parse example due to OAS2 incompatibility: {}", definition.getJsonPointer().toPrettyString());
            } else {
                throw e;
            }
        }
    }

    private static List<String> getLines(File file) throws IOException {
        var lines = Files.readAllLines(file.toPath());
        if (lines.size() < 1)
            throw new RuntimeException("[Internal error] File: " + file.getName() + " appears to be empty!");
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
        Path basePath = java.nio.file.Paths.get(file.getParent());
        Set<String> refs = getExternalReferencesFromFile(file);
        for (String ref : refs) {
            File refFile = resolveRelativeFile(ref, basePath);
            if (files.add(refFile)) {
                resolveReferences(refFile, files);
            }
        }
    }

    private static File resolveRelativeFile(String relativePath, Path basePath) {
        File refFile = new File(String.valueOf(basePath.resolve(relativePath).normalize()));
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
        openApiFiles.put(file.getAbsolutePath(), readOpenApiFile(file));
        Set<File> refFiles = getReferencedFiles(file);
        for (File refFile : refFiles) {
            openApiFiles.put(refFile.getAbsolutePath(), readOpenApiFile(refFile));
        }
        return openApiFiles;
    }

    private SourceDefinition readOpenApiFile(File file) throws IOException {
        return new SourceDefinition(file, buildOpenApiSpecification(file));
    }

}
