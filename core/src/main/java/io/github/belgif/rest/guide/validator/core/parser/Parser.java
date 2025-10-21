package io.github.belgif.rest.guide.validator.core.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.github.belgif.rest.guide.validator.LineRangePath;
import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
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
import java.nio.file.Paths;
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
        private File openApiFile;
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
        }

        private void populateModelDefinitionMap() {
            allDefinitions.forEach(def -> modelDefinitionMap.put(def.getModel(), def));
        }

        @SuppressWarnings("unchecked")
        public <T extends Constructible> OpenApiDefinition<T> resolve(T model) {
            if (model instanceof Reference<?> reference) {
                var ref = reference.getRef();
                if (ref != null) {
                    return (OpenApiDefinition<T>) resolveReference(reference).orElseThrow(() -> new RuntimeException("[Parsing error] Could not find match of " + ref));
                }
            }

            // no ref
            if (modelDefinitionMap.containsKey(model)) {
                return (OpenApiDefinition<T>) modelDefinitionMap.get(model);
            } else {
                throw new RuntimeException("[Internal error] Could not find match of " + model.toString());
            }
        }

        private Optional<OpenApiDefinition<?>> resolveReference(Reference<?> reference) {
            var ref = reference.getRef();
            File refOpenApiFile = findOpenApiFileForRef(ref, reference);
            JsonPointer pointer = buildJsonPointerFromRef(ref);
            return allDefinitions.stream().filter(def ->
                            def.getJsonPointer().equals(pointer) && def.getOpenApiFile().equals(refOpenApiFile))
                    .findFirst();
        }

        public Optional<SchemaDefinition> resolveDiscriminatorMapping(SchemaDefinition schema, String mapping) {
            if (mapping.contains("#/") || mapping.contains(".")) {
                File refOpenApiFile = findOpenApiFileForRef(mapping, schema.getModel());
                JsonPointer pointer = buildJsonPointerFromRef(mapping);
                return schemas.stream().filter(def ->
                                def.getJsonPointer().equals(pointer) && def.getOpenApiFile().equals(refOpenApiFile))
                        .findFirst();
            }
            return schemas.stream().filter(def ->
                            def.getOpenApiFile().equals(schema.getOpenApiFile()) &&
                                    def.getIdentifier() != null &&
                                    def.getIdentifier().equals(mapping))
                    .findFirst();
        }

        private JsonPointer buildJsonPointerFromRef(String ref) {
            ref = !ref.startsWith("#") ? ref.split("#")[1] : ref.substring(1);
            return new JsonPointer(ref);
        }

        private File findOpenApiFileForRef(String ref, Reference<?> refModel) {
            Constructible model = (Constructible) refModel;
            File modelBaseFile = modelDefinitionMap.get(model).getOpenApiFile();
            String fileRef = ref.split("#")[0];
            if (fileRef.isEmpty()) {
                return modelBaseFile;
            }
            Path basePath = java.nio.file.Paths.get(modelBaseFile.getParent());
            return resolveRelativeFile(fileRef, basePath);
        }

        public void populateBackReferences() {
            for (OpenApiDefinition<?> def : allDefinitions) {
                if (def.getModel() instanceof Reference && ((Reference<?>) def.getModel()).getRef() != null) {
                    resolve(def.getModel()).addBackReference(def);
                }
            }
            for (SchemaDefinition schemaDefinition : schemas) {
                if (schemaDefinition.getModel().getDiscriminator() != null &&
                        schemaDefinition.getModel().getDiscriminator().getMapping() != null) {
                    for (String mapping : schemaDefinition.getModel().getDiscriminator().getMapping().values()) {
                        var optional = resolveDiscriminatorMapping(schemaDefinition, mapping);
                        optional.ifPresent(definition -> definition.addBackReference(schemaDefinition));
                    }
                }
            }
        }
    }

    private void parseOpenApiFiles(ParserResult result) throws IOException {
        result.src = new HashMap<>();
        Set<File> foundReferencedFiles = new HashSet<>();
        Set<File> filesProcessed = new HashSet<>();
        foundReferencedFiles.add(result.openApiFile.getAbsoluteFile());
        while (!filesProcessed.equals(foundReferencedFiles)) {
            Set<SourceDefinition> sourcesToProcess = new HashSet<>();
            for (File file : foundReferencedFiles.stream().filter(f -> !filesProcessed.contains(f)).collect(Collectors.toSet())) {
                // readOpenApiFile not incorporated in the stream because it throws a checked exception that needs to be thrown further
                sourcesToProcess.add(new SourceDefinition(file, buildOpenApiSpecification(file)));
            }
            for (SourceDefinition sourceDefinition : sourcesToProcess) {
                result.src.put(sourceDefinition.getFile().getAbsolutePath(), sourceDefinition);
                parseDefinitions(result, sourceDefinition);
                result.assembleAllDefinitions();
                foundReferencedFiles.addAll(findExternalReferences(result, sourceDefinition));
                filesProcessed.add(sourceDefinition.getFile());
            }
        }
        result.populateModelDefinitionMap();
    }

    public ParserResult parse(ViolationReport violationReport) {
        try {
            var result = new ParserResult();
            result.openApiFile = openApiFile;
            parseOpenApiFiles(result);
            if (!isOasVersionSupported(result.src.values(), violationReport)) {
                return null;
            }
            verifySecurityRequirements(result);
            validateAllReferences(result);
            if (!result.isParsingValid()) {
                // Double log because: The exception message is a bit separated from the parsing errors in the output, and only added to the end of some long output line.
                log.error("Input file is not a valid OpenAPI document. Compliance to the REST style guidelines could not be verified.");
                throw new RuntimeException("Input file is not a valid OpenAPI document. Compliance to the REST style guidelines could not be verified.");
            }
            result.populateBackReferences();
            buildAllPathWithLineRange(result);
            return result;
        } catch (IOException e) {
            violationReport.addViolation(e.getClass().getSimpleName(), e.getLocalizedMessage(), new Line(openApiFile.getName(), 0), "#");
            return null;
        }
    }

    private void parseDefinitions(ParserResult result, SourceDefinition sourceDefinition) {
        parsePaths(sourceDefinition, result);
        parseComponents(sourceDefinition, result);
        parseGlobalSecurityRequirements(sourceDefinition, result);
        if (sourceDefinition.getFile().equals(result.openApiFile)) {
            result.openAPI = sourceDefinition.getOpenApi();
            parseServers(result);
        }
    }

    private static boolean isOasVersionSupported(Collection<SourceDefinition> sources, ViolationReport violationReport) {
        Set<SourceDefinition> invalidSources = sources.stream().filter(sourceDefinition -> getOasVersion(sourceDefinition) == 2).collect(Collectors.toSet());
        if (invalidSources.isEmpty()) {
            return true;
        } else {
            invalidSources.forEach(sourceDefinition -> violationReport.addViolation("[unsupported]", "Input files of type OpenApi version 2.0 / Swagger 2.0 are not supported. Only OpenAPI 3.0 documents are supported", sourceDefinition.getFileName()));
        }
        return false;
    }

    public void verifySecurityRequirements(ParserResult result) {
        var allowedRequirements = result.getSecuritySchemes().stream().map(OpenApiDefinition::getIdentifier).collect(Collectors.toSet());
        result.securityRequirements.forEach(securityRequirement -> {
            for (String securityScheme : securityRequirement.getModel().getSchemes().keySet()) {
                if (!allowedRequirements.contains(securityScheme)) {
                    log.error("{}: Security Scheme <<{}>> is not defined", securityRequirement.getFullyQualifiedPointer(), securityScheme);
                    result.setParsingValid(false);
                }
            }
        });
    }

    private void validateAllReferences(ParserResult result) {
        for (OpenApiDefinition<?> def : result.allDefinitions) {
            if (def.getModel() instanceof Reference<?> ref && ref.getRef() != null) {
                var optional = result.resolveReference(ref);
                if (optional.isEmpty()) {
                    log.error("[Parsing error] Could not find match of {}", ref.getRef());
                    result.setParsingValid(false);
                }
            }
        }
        for (SchemaDefinition schemaDefinition : result.schemas) {
            if (schemaDefinition.getModel().getDiscriminator() != null &&
                    schemaDefinition.getModel().getDiscriminator().getMapping() != null) {
                for (String mapping : schemaDefinition.getModel().getDiscriminator().getMapping().values()) {
                    var optional = result.resolveDiscriminatorMapping(schemaDefinition, mapping);
                    if (optional.isEmpty()) {
                        log.error("[Parsing error] Could not find match of discriminator mapping: {} in: {}", mapping, schemaDefinition.getFullyQualifiedPointer());
                        result.setParsingValid(false);
                    }
                }
            }
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
            String version;
            var openApiLine = getLines(file).stream().filter(line -> line.trim().startsWith("openapi: ")).findFirst();
            if (openApiLine.isPresent()) {
                version = openApiLine.get().substring(9);
            } else {
                throw new RuntimeException("Input file is not an OpenApi file, or version number could not be found. <<" + file.getAbsolutePath() + ">>");
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
        var openApi = sourceDefinition.getFile();
        if (paths == null) {
            return;
        }
        PathsDefinition pathsDefinition = new PathsDefinition(paths, openApi, result);
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
        var openApi = sourceDefinition.getFile();
        if (components == null) {
            return;
        }
        var responses = components.getResponses();
        if (responses != null) {
            responses.forEach((name, response) -> {
                var responseDef = new ResponseDefinition(response, name, openApi, result);
                result.responses.add(responseDef);
                parseResponse(responseDef, result);
            });
        }

        var requestBodies = components.getRequestBodies();
        if (requestBodies != null) {
            requestBodies.forEach((name, requestBody) -> {
                var requestBodyDef = new RequestBodyDefinition(requestBody, name, openApi, result);
                result.requestBodies.add(requestBodyDef);
                parseRequestBody(requestBodyDef, result);
            });
        }

        var schemas = components.getSchemas();
        if (schemas != null) {
            schemas.forEach((name, schema) -> {
                var schemaDef = new SchemaDefinition(schema, name, openApi, result);
                result.schemas.add(schemaDef);
                parseSchema(schemaDef, result);
            });
        }

        var headers = components.getHeaders();
        if (headers != null) {
            headers.forEach((name, header) -> {
                var headerDef = new ResponseHeaderDefinition(header, name, openApi, result);
                result.headers.add(headerDef);
                parseHeaders(headerDef, result);
            });
        }
        var parameters = components.getParameters();
        if (parameters != null) {
            parameters.forEach((name, parameter) -> {
                var parameterDef = new ParameterDefinition(parameter, name, openApi, result);
                result.parameters.add(parameterDef);
                parseParameter(parameterDef, result);
            });
        }
        var examples = components.getExamples();
        if (examples != null) {
            examples.forEach((name, example) -> {
                var exampleDef = new ExampleDefinition(example, name, openApi, result);
                result.examples.add(exampleDef);
            });
        }
        var securitySchemes = components.getSecuritySchemes();
        if (securitySchemes != null) {
            securitySchemes.forEach((name, scheme) -> result.securitySchemes.add(new SecuritySchemeDefinition(scheme, name, openApi, result)));
        }
        var links = components.getLinks();
        if (links != null) {
            links.forEach((name, link) -> result.links.add(new LinkDefinition(link, name, openApi, result)));
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
    }

    private Set<File> findExternalReferences(ParserResult result, SourceDefinition sourceDefinition) {
        Set<File> externalFiles = new HashSet<>();
        externalFiles.addAll(
                result.getAllDefinitions().stream()
                        .filter(def -> def.getOpenApiFile().equals(sourceDefinition.getFile()))
                        .filter(def -> def.getModel() instanceof Reference && ((Reference<?>) def.getModel()).getRef() != null)
                        .map(def -> ((Reference<?>) def.getModel()).getRef())
                        .filter(Parser::isExternalReference).map(ref -> ref.split("#")[0])
                        .map(externalFile -> resolveRelativeFile(externalFile, Paths.get(sourceDefinition.getFile().getParent())))
                        .map(File::getAbsoluteFile)
                        .collect(Collectors.toSet()));
        externalFiles.addAll(
                result.getSchemas().stream().filter(def -> def.getOpenApiFile().equals(sourceDefinition.getFile()))
                        .filter(def -> def.getModel().getDiscriminator() != null && def.getModel().getDiscriminator().getMapping() != null)
                        .flatMap(def -> def.getModel().getDiscriminator().getMapping().values().stream())
                        .filter(Parser::isExternalReference).filter(ref -> ref.contains(".")).map(ref -> ref.split("#")[0])
                        .map(externalFile -> resolveRelativeFile(externalFile, Paths.get(sourceDefinition.getFile().getParent())))
                        .map(File::getAbsoluteFile)
                        .collect(Collectors.toSet()));
        return externalFiles;
    }

    private static List<String> getLines(File file) throws IOException {
        var lines = Files.readAllLines(file.toPath());
        if (lines.isEmpty())
            throw new RuntimeException("[Internal error] File: " + file.getName() + " appears to be empty!");
        // lines > 1 then is a yaml or a pretty json file
        if (lines.size() > 1) return lines;

        // else is a ugly json file
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var pretty = gson.toJson(JsonParser.parseString(lines.get(0)));
        return pretty.lines().toList();
    }

    private static File resolveRelativeFile(String relativePath, Path basePath) {
        File refFile = new File(String.valueOf(basePath.resolve(relativePath).normalize()));
        if (refFile.exists() && refFile.isFile()) {
            return refFile;
        } else {
            throw new RuntimeException("File not found: " + refFile.getAbsolutePath());
        }
    }

    private static boolean isExternalReference(String ref) {
        return !ref.startsWith("#");
    }

}
