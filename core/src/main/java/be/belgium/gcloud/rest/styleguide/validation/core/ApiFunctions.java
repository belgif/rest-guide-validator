package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.LineRangePath;
import be.belgium.gcloud.rest.styleguide.validation.core.model.OpenApiDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.model.PathDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.model.SchemaDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.openapitools.empoa.swagger.core.internal.SwAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

        last.setEnd(getEndOfObjectLineNumber("paths", oas, 1));

        return paths;
    }

    //TODO refactor to take in JsonPointer or refactor exclusion completely
    private static int getEndOfObjectLineNumber(String objectName, OpenApiViolationAggregator oas, int nestedLevelWanted) {
        File file = oas.getOpenApiFile();
        JsonFactory factory;
        if (file.getName().endsWith("yaml") || file.getName().endsWith("yml")) {
            factory = new YAMLFactory();
        } else {
            factory = new JsonFactory();
        }

        try {
            com.fasterxml.jackson.core.JsonParser jsonParser = factory.createParser(file);
            boolean objectFound = false;
            int nestedObjectCounter = 0;
            while (!jsonParser.isClosed()) {
                jsonParser.nextToken();
                if (jsonParser.getCurrentToken() == JsonToken.FIELD_NAME && !objectFound) {
                    if (objectName.equals(jsonParser.getCurrentName())) {
                        objectFound = true;
                        continue;
                    }
                }
                if (jsonParser.getCurrentToken() == JsonToken.START_OBJECT || jsonParser.getCurrentToken() == JsonToken.START_ARRAY) {
                    nestedObjectCounter++;
                }
                if ((jsonParser.getCurrentToken() == JsonToken.END_OBJECT || jsonParser.getCurrentToken() == JsonToken.END_ARRAY) && nestedObjectCounter > 0) {
                    nestedObjectCounter--;
                }
                if (objectFound && jsonParser.getCurrentToken() == JsonToken.END_OBJECT && nestedObjectCounter == nestedLevelWanted) {
                    return jsonParser.getCurrentLocation().getLineNr();
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException("Could not parse " + file.getName() + " for linenumber calculation", ex);
        }
        return 0;
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
                if (schema.getProperties() != null && schema.getProperties().containsKey("items") && schema.getProperties().get("items").getType().equals(Schema.SchemaType.ARRAY)) {
                    isCollection.set(true);
                }
            });
        } catch (NullPointerException ex) {
            return false;
        }
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