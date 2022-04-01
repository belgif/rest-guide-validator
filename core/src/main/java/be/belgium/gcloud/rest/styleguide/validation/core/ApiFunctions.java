package be.belgium.gcloud.rest.styleguide.validation.core;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.openapitools.empoa.swagger.core.internal.SwAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ApiFunctions {

    /**
     * avoid instance creation.
     */
    private ApiFunctions(){}

    public static OpenAPI buildOpenApiSpecification(File file, OpenApiViolationAggregator oas) throws IOException {
        oas.setOpenApiFile(file);
        oas.setSrc( Files.readAllLines(file.toPath()));

        var openApiParser = new OpenAPIParser();
        var parseOptions = new ParseOptions();
        parseOptions.setResolve(true);

        var parserResult = openApiParser.readLocation(file.getAbsolutePath(), null, parseOptions);
        var openAPI = parserResult.getOpenAPI();
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

    public static Set<String> getPathMatch(OpenAPI openAPI, String pattern){
        return getPathKeys(openAPI).stream().filter(k-> k.matches(pattern)).collect(Collectors.toSet());
    }
    public static Set<String> getPathNoMatch(OpenAPI openAPI, String pattern){
        return getPathKeys(openAPI).stream().filter(k-> ! k.matches(pattern)).collect(Collectors.toSet());
    }

    public static Set<String> getPathKeys(OpenAPI openAPI) {
        return openAPI.getPaths().getPathItems().keySet();
    }
    public static Paths getPaths(OpenAPI api){
        return api.getPaths();
    }

    public static Set<String> getOperationId(OpenAPI api, OperationEnum verb, String statusCode){
        return  api.getPaths().getPathItems().values().stream()
                .filter(path -> filterPath(path, verb, statusCode))
                .map(path -> getOperationId(path, verb))
                .collect(Collectors.toSet());
    }

    private static boolean filterPath(PathItem path, OperationEnum verb, String statusCode){
        switch (verb){
            case GET: return path.getGET() != null && path.getGET().getResponses().getAPIResponses().keySet().contains(statusCode);
            case POST: return path.getPOST() != null && path.getPOST().getResponses().getAPIResponses().keySet().contains(statusCode);
            case PUT: return path.getPUT() != null &&  path.getPUT().getResponses().getAPIResponses().keySet().contains(statusCode);
            case DELETE: return path.getDELETE() != null &&  path.getDELETE().getResponses().getAPIResponses().keySet().contains(statusCode);
            case PATCH: return path.getPATCH() != null &&  path.getPATCH().getResponses().getAPIResponses().keySet().contains(statusCode);
            case HEAD: return path.getHEAD() != null &&  path.getHEAD().getResponses().getAPIResponses().keySet().contains(statusCode);
            case OPTIONS: return path.getOPTIONS() != null && path.getOPTIONS().getResponses().getAPIResponses().keySet().contains(statusCode);
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
    public static Map<String, List<String>> getDefinitionPropertiesNoMatch(OpenAPI api, String pattern) {
        Map<String, List<String>> result = new HashMap<>();

        for (String k : api.getComponents().getSchemas().keySet()) {
            addNotMatch(pattern, result, api.getComponents().getSchemas().get(k).getProperties(), k);
        }
        return result;
    }

    public static Set<String> getServerNotMatch(OpenAPI api, String regex) {
        return api.getServers().stream()
                .map(Server::getUrl)
                .filter(url -> ! url.matches(regex))
                .collect(Collectors.toSet());
    }

    public static List<ComponentProperties> getPropertiesNotMatch(OpenAPI api, String regex){
        List<ComponentProperties> properties = new ArrayList<>();
        api.getComponents().getSchemas().forEach((k,v) ->{
            if(v.getProperties() != null)
                v.getProperties().keySet().stream()
                        .filter(s-> ! s.matches(regex))
                        .forEach(prop ->properties.add(new ComponentProperties(ComponentType.SCHEMA, k, prop)));
        });
        return properties;
    }

}