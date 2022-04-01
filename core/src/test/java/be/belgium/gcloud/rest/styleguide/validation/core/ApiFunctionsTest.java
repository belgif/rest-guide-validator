package be.belgium.gcloud.rest.styleguide.validation.core;

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
        File file = new File(getClass().getResource("../rules/swagger_bad.yaml").getFile());
        OpenAPI openApi = ApiFunctions.buildOpenApiSpecification(file, new OpenApiViolationAggregator());
        return openApi;
    }

    @Test
    void buildOpenApiSpecification()throws IOException {
        assertNotNull(getOpenApi());
    }

    @Test
    void getOperationId() throws IOException{
        Set<String> operationIds = ApiFunctions.getOperationId(getOpenApi(), OperationEnum.GET, "200");
        assertNotNull(operationIds);
        assertTrue(operationIds.size() > 0);
    }

    @Test
    void getPathKeys()throws IOException {
        Set<String> keys = ApiFunctions.getPathKeys(getOpenApi());
        assertNotNull(keys);
        assertFalse(keys.isEmpty());
    }

    @Test
    void getPaths() throws IOException{
        Paths paths = ApiFunctions.getPaths(getOpenApi());
        assertNotNull(paths);
        assertFalse(paths.getPathItems().isEmpty());
    }

    @Test
    void testGetDefinitionPropertiesNoMatch() throws IOException {
        Map<String, List<String>> stringListMap = ApiFunctions.getDefinitionPropertiesNoMatch(getOpenApi(),  "^[a-z]+([A-Z]?[a-z]+)*$");
        assertNotNull(stringListMap);
        assertFalse(stringListMap.isEmpty());
        stringListMap.keySet().forEach(System.out::println);
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
}