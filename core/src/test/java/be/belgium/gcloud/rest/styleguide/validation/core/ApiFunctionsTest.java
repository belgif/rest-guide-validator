package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.core.jsonpath.ApiPathFunctions;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ApiFunctionsTest {
    private OpenAPI getOpenApi() throws IOException {
        var file = new File(getClass().getResource("../rules/swagger_bad.yaml").getFile());
        var openApi = ApiFunctions.buildOpenApiSpecification(file, new OpenApiViolationAggregator());
        return openApi;
    }

    @Test
    void buildOpenApiSpecification()throws IOException {
        assertNotNull(getOpenApi());
    }

    @Test
    void buildUgly()throws IOException {
        var oas = new OpenApiViolationAggregator();
        var file = new File(getClass().getResource("../rules/ugly.json").getFile());
        var openApi = ApiFunctions.buildOpenApiSpecification(file, oas);
        assertNotNull(openApi);
        assertTrue(oas.src.size()>1);
    }

    @Test
    void getOperationId() throws IOException{
        var operationIds = ApiFunctions.getOperationId(getOpenApi(), OperationEnum.GET, "200");
        assertNotNull(operationIds);
        assertTrue(operationIds.size() > 0);
    }

    @Test
    void getPathKeys()throws IOException {
        var keys = ApiFunctions.getPathKeys(getOpenApi());
        assertNotNull(keys);
        assertFalse(keys.isEmpty());
    }

    @Test
    void getPaths() throws IOException{
        var paths = ApiFunctions.getPaths(getOpenApi());
        assertNotNull(paths);
        assertFalse(paths.getPathItems().isEmpty());
    }

    @Test
    void testGetDefinitionPropertiesNoMatch() throws IOException {
        var stringListMap = ApiFunctions.getDefinitionPropertiesNoMatch(getOpenApi(),  "^[a-z]+([A-Z]?[a-z]+)*$");
        assertNotNull(stringListMap);
        assertFalse(stringListMap.isEmpty());
    }

    @Test
    void getServerNotMatch() throws IOException {
        var pattern = "^((https:\\/\\/)|\\/?)[-a-zA-Z0-9@:%._\\+~#=]{1,256}[a-zA-Z0-9()]{1,6}\\/[A-Za-z0-9]*(\\/[a-z0-9]+([A-Z]?[a-z0-9]+)*)*\\/v[0-9]+$";
        var servers = ApiFunctions.getServerNotMatch(getOpenApi(), pattern);
        assertNotNull(servers);
        assertFalse(servers.isEmpty());
    }
    @Test
    void testUrl() {
        var pattern = "^((https:\\/\\/)|\\/?)[-a-zA-Z0-9@:%._\\+~#=]{1,256}[a-zA-Z0-9()]{1,6}\\/[A-Za-z0-9]*(\\/[a-z0-9]+([A-Z]?[a-z0-9]+)*)*\\/v[0-9]+$";
        var url = "https://localhost:8080/REST/v1";
        assertTrue(url.matches(pattern));
        url = "/REST/contactData/v1";
        assertTrue(url.matches(pattern));
    }

    @Test
    void testProperties()throws IOException {
        var properties = ApiFunctions.getPropertiesNotMatch(getOpenApi(), "^[a-z]+([A-Z]?[a-z0-9]+)*$");
        assertNotNull(properties);
        assertFalse(properties.isEmpty());
    }

    @Test
    void getAllPathWithLineRange() throws IOException {
        var oas = new OpenApiViolationAggregator();
        var file = new File(getClass().getResource("../rules/swagger_bad.yaml").getFile());
        var openApi = ApiFunctions.buildOpenApiSpecification(file, oas);

        var paths = ApiFunctions.getAllPathWithLineRange(openApi, oas);
        assertNotNull(paths);
        paths.forEach(p-> assertTrue(p.getEnd() > p.getStart()));
        paths.forEach(p-> log.debug(p.toString()));
    }

    @Test
    void isInPathList() throws IOException {
        var oas = new OpenApiViolationAggregator();
        var file = new File(getClass().getResource("../rules/swagger_bad.yaml").getFile());
        var openApi = ApiFunctions.buildOpenApiSpecification(file, oas);

        var paths = ApiFunctions.getAllPathWithLineRange(openApi, oas);
        assertTrue(ApiFunctions.isInPathList(paths, "/health", 2070));
    }
}