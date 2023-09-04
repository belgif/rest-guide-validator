package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.LineRangePath;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
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
    private ApiFunctions(){}

    /**
     * Read a file and store each line in a list.
     * If the file has only one line this function use a GSon librairy to build a pretty list of line.
     * @param file must be a yaml or a json file
     * @return a list of line
     * @throws IOException
     */
    private static List<String> getLines(File file) throws IOException {
        var lines = Files.readAllLines(file.toPath());

        // lines > 1 then is a yaml or a pretty json file
        if(lines.size() > 1)
            return lines;

        // else is a ugly json file
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var pretty = gson.toJson( JsonParser.parseString(lines.get(0)) );
        return pretty.lines().collect(Collectors.toList());
    }

    /**
     * Build the java object structure from the file.
     * @side-effect add the file and a list of line from the file to oas.
     * @param file a openApi yaml or json file.
     * @param oas OpenApiViolationAggregator used to add file and list of line from the file.
     * @return OpenAPI, the java object structure
     * @throws IOException
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
        if(openAPI == null){
            openAPI = new io.swagger.v3.oas.models.OpenAPI();
            var version = getLines(file).stream()
                                                     .filter(line -> line.trim().startsWith("openapi: "))
                                                     .findFirst().orElseThrow()
                                                     .substring(9);
            openAPI.setOpenapi(version);
        }
        return SwAdapter.toOpenAPI(openAPI);
    }

    private static void addNotMatch(String pattern, Map<String, List<String>> result, Map<String, Schema> properties, String k) {
        if (properties != null) {
            List<String> list = new LinkedList<>();
            properties.keySet().forEach(s -> {
                if (!s.matches(pattern)) {
                    list.add(s);
                }
            });
            if (!list.isEmpty())
                result.put(k, list);
        }
    }

    /**
     * In the openAPI get all Path keys that match the pattern.
     * @param openAPI
     * @param pattern
     * @return a set path keys
     */
    public static Set<String> getPathMatch(OpenAPI openAPI, String pattern){
        return getPathKeys(openAPI).stream().filter(k-> k.matches(pattern)).collect(Collectors.toSet());
    }

    /**
     * In the openAPI get all Path keys that NOT match the pattern.
     * @param openAPI
     * @param pattern
     * @return a set path keys
     */
    public static Set<String> getPathNoMatch(OpenAPI openAPI, String pattern){
        return getPathKeys(openAPI).stream().filter(k-> ! k.matches(pattern)).collect(Collectors.toSet());
    }

    /**
     * Return all path key for openAPI
     * @param openAPI
     * @return a set path keys
     */
    public static Set<String> getPathKeys(OpenAPI openAPI) {
        if(openAPI.getPaths()==null || openAPI.getPaths().getPathItems()==null)
            return Collections.emptySet();
        return openAPI.getPaths().getPathItems().keySet();
    }

    /**
     * Return all path for openAPI
     * @param api
     * @return
     */
    public static Paths getPaths(OpenAPI api){
        return api.getPaths();
    }

    /**
     * For the openapi get all path.operationId that match the verb and the status code.
     * @param openAPI
     * @param verb
     * @param statusCode
     * @return
     */
    public static Set<String> getOperationId(OpenAPI openAPI, OperationEnum verb, String statusCode){
        if(openAPI.getPaths()==null || openAPI.getPaths().getPathItems()==null)
            return Collections.emptySet();
        return  openAPI.getPaths().getPathItems().values().stream()
                .filter(path -> filterPath(path, verb, statusCode))
                .map(path -> getOperationId(path, verb))
                .collect(Collectors.toSet());
    }

    private static boolean filterPath(PathItem path, OperationEnum verb, String statusCode){
        switch (verb){
            case GET: return path.getGET() != null && path.getGET().getResponses().getAPIResponses().containsKey(statusCode);
            case POST: return path.getPOST() != null && path.getPOST().getResponses().getAPIResponses().containsKey(statusCode);
            case PUT: return path.getPUT() != null &&  path.getPUT().getResponses().getAPIResponses().containsKey(statusCode);
            case DELETE: return path.getDELETE() != null &&  path.getDELETE().getResponses().getAPIResponses().containsKey(statusCode);
            case PATCH: return path.getPATCH() != null &&  path.getPATCH().getResponses().getAPIResponses().containsKey(statusCode);
            case HEAD: return path.getHEAD() != null &&  path.getHEAD().getResponses().getAPIResponses().containsKey(statusCode);
            case OPTIONS: return path.getOPTIONS() != null && path.getOPTIONS().getResponses().getAPIResponses().containsKey(statusCode);
            default: throw new IllegalArgumentException("unknow verb: "+verb);
        }
    }
    private static String getOperationId(PathItem path, OperationEnum verb){
        switch (verb){
            case GET: return path.getGET().getOperationId();
            case POST: return path.getPOST().getOperationId();
            case PUT: return  path.getPUT().getOperationId();
            case DELETE: return path.getDELETE().getOperationId();
            case PATCH: return path.getPATCH().getOperationId();
            case HEAD: return path.getHEAD().getOperationId();
            case OPTIONS: return path.getOPTIONS().getOperationId();
            default: throw new IllegalArgumentException("unknow verb: "+verb);
        }
    }

    /**
     *
     * @param pattern
     * @return Map<String, String> The key is the definition name and the value is the property name
     */
    public static Map<String, List<String>> getDefinitionPropertiesNoMatch(OpenAPI openAPI, String pattern) {
        if(openAPI.getComponents()==null || openAPI.getComponents().getSchemas()==null)
            return Collections.EMPTY_MAP;
        Map<String, List<String>> result = new HashMap<>();

        for (String k : openAPI.getComponents().getSchemas().keySet()) {
            addNotMatch(pattern, result, openAPI.getComponents().getSchemas().get(k).getProperties(), k);
        }
        return result;
    }

    /**
     * For openAPI, get all server.url that NOT match the regex.
     * @param openAPI
     * @param regex
     * @return
     */
    public static Set<String> getServerNotMatch(OpenAPI openAPI, String regex) {
        if(openAPI.getServers()==null)
            return Collections.emptySet();
        return openAPI.getServers().stream()
                .map(Server::getUrl)
                .filter(url -> ! url.matches(regex))
                .collect(Collectors.toSet());
    }

    /**
     * For openAPI get all component.schema.properties that NOT match the regex.
     * @param openAPI
     * @param regex
     * @return
     */
    public static List<ComponentProperties> getPropertiesNotMatch(OpenAPI openAPI, String regex){
        List<ComponentProperties> properties = new ArrayList<>();
        if(openAPI.getComponents()!=null && openAPI.getComponents().getSchemas()!=null)
            openAPI.getComponents().getSchemas().forEach((k,v) ->{
                if(v.getProperties() != null)
                    v.getProperties().keySet().stream()
                            .filter(s-> ! s.matches(regex))
                            .forEach(prop ->properties.add(new ComponentProperties(ComponentType.SCHEMA, k, prop)));
            });
        return properties;
    }

    /**
     * For all openAPI.path.key build a LineRangePath whith the start and end line of the path.
     * @pre: Assume that in the file, after the paths we found nothing or a next element in ("components", "security", "securityDefinitions", "definitions", "externalDocs")
     * @param openAPI
     * @param oas
     * @return
     */
    public static List<LineRangePath> buildAllPathWithLineRange(OpenAPI openAPI, OpenApiViolationAggregator oas){
        var paths = new ArrayList<LineRangePath>();
        var pathKeys = getPathKeys(openAPI);

        if(pathKeys.isEmpty())
            return Collections.emptyList();

        pathKeys.forEach(p-> paths.add(new LineRangePath(p, oas.getLineNumber(p))));
        Collections.sort(paths);
        for(int i=0; i<pathKeys.size()-1;i++){
            paths.get(i).setEnd(paths.get(i+1).getStart()-1);
        }

        var last = paths.get(paths.size()-1);

        var others = List.of(new String[]{"components", "security", "securityDefinitions", "definitions", "externalDocs"});
        var otherRanges = others.stream()
                .map(other -> new LineRangePath(other, oas.getLineNumber(other)))
                .filter(range -> range.getStart() > last.getStart())
                .sorted()
                .collect(Collectors.toList());
        if (otherRanges.isEmpty())
            last.setEnd(oas.src.size()-1);
        else
            last.setEnd(otherRanges.get(0).getStart());

        return  paths;
    }

    /**
     * Return true if at least one element that {LineRangePath.path == path and LineRangePath.start >= lineNumber < LineRangePath.end}
     * @param lineRangePaths
     * @param path
     * @param lineNumber
     * @return
     */
    public static boolean isInPathList(List<LineRangePath> lineRangePaths, String path, int lineNumber){
        return lineRangePaths.stream()
                .anyMatch(lineRangePath -> lineRangePath.getPath().equals(path) && lineRangePath.inRange(lineNumber));
    }

    /**
     * Return true if at least one element that {LineRangePath.path IN paths and LineRangePath.start >= lineNumber < LineRangePath.end}
     * @param lineRangePaths
     * @param paths
     * @param lineNumber
     * @return
     */
    public static boolean isInPathList(List<LineRangePath> lineRangePaths, List<String> paths, int lineNumber){
        return lineRangePaths.stream()
                .anyMatch(lineRangePath -> paths.contains(lineRangePath.getPath()) && lineRangePath.inRange(lineNumber));
    }

    /**
     * Get a list of pathkey that return a collection.
     * @param openAPI
     * @return
     */
    public static List<String> getReturnCollectionPathKey(OpenAPI openAPI){
        return new ArrayList<>(getCollectionPathItems(openAPI).keySet());
    }

    public static Map<String, PathItem> getCollectionPathItems(OpenAPI openAPI) {
        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems().size() == 0) {
            return new HashMap<>();
        }
        var allPaths = openAPI.getPaths().getPathItems().entrySet();
        // Adds all paths before the ones with path params
        Set<String> collectionPaths = allPaths.stream().filter(path -> endsWithPathParameter(path.getKey()))
                .map(path -> getPathBeforePathParam(path, openAPI)).filter(Objects::nonNull).collect(Collectors.toSet());
        // Adds all paths that return an object with an array 'items'
        collectionPaths.addAll(allPaths.stream().filter(path -> isReturnCollection(openAPI, path.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet()));

        // Filter out all collections without GET
        var pathsWithGet = allPaths.stream()
                .filter(path -> collectionPaths.contains(path.getKey()) &&
                    path.getValue().getOperations().containsKey(PathItem.HttpMethod.GET))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue ));
        return pathsWithGet;
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

    public static boolean isReturnCollection(OpenAPI openAPI, PathItem pathItem){
        try{
            AtomicBoolean isCollection = new AtomicBoolean(false);
            var responseSchemas = pathItem.getGET().getResponses().getAPIResponses().values().stream()
                    .flatMap(apiResponse -> apiResponse.getContent().getMediaTypes().values().stream())
                    .map(MediaType::getSchema);
            responseSchemas.forEach(schema -> {
                if (schema.getProperties()!=null && schema.getProperties().containsKey("items")
                        && schema.getProperties().get("items").getType().equals(Schema.SchemaType.ARRAY)){
                    isCollection.set(true);
                }
                else {
                    if(isCollection(openAPI, schema.getRef()))
                        isCollection.set(true);
                }
            });
            return isCollection.get();
        }catch (NullPointerException ex){
            return false;
        }
    }

    private static boolean isCollection(OpenAPI openAPI, String ref){
        try{
            if( ! ref.startsWith("#")){
                log.debug("Cannot check an external reference.");
                return false;
            }
            return openAPI.getComponents().getSchemas().get(getRefName(ref)).getProperties().get("items").getType().equals(Schema.SchemaType.ARRAY);

        }catch (NullPointerException ex){
            return false;
        }
    }

    public static List<Operation> getOperations(OpenAPI api, OperationEnum[] exclude) {
        return getOperationsToCheck(api.getPaths().getPathItems().values(), exclude);
    }

    private static List<Operation> getOperationsToCheck(Collection<PathItem> pathItems, OperationEnum[] exclude) {
        // Does not check for options, head or trace
        List<Operation> operations = new ArrayList<>();
        List<OperationEnum> verbs = Arrays.asList(exclude);
        for (PathItem pathItem : pathItems) {
            if (pathItem.getGET() != null && !verbs.contains(OperationEnum.GET)) {
                operations.add(pathItem.getGET());
            }
            if (pathItem.getPUT() != null && !verbs.contains(OperationEnum.PUT)) {
                operations.add(pathItem.getPUT());
            }
            if (pathItem.getPOST() != null && !verbs.contains(OperationEnum.POST)) {
                operations.add(pathItem.getPOST());
            }
            if (pathItem.getDELETE() != null && !verbs.contains(OperationEnum.DELETE)) {
                operations.add(pathItem.getDELETE());
            }
            if (pathItem.getPATCH() != null && !verbs.contains(OperationEnum.PATCH)) {
                operations.add(pathItem.getPATCH());
            }
            if (pathItem.getHEAD() != null && !verbs.contains(OperationEnum.HEAD)) {
                operations.add(pathItem.getHEAD());
            }
            if (pathItem.getOPTIONS() != null && !verbs.contains(OperationEnum.OPTIONS)) {
                operations.add(pathItem.getOPTIONS());
            }
        }
        return operations;
    }

    private static String getRefName(String ref){
        if( ! ref.contains("/"))
            return ref;
        return ref.substring(ref.lastIndexOf('/')+1);
    }
}