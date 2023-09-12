package be.belgium.gcloud.rest.styleguide.validation.core;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
    void testUrlV3() throws IOException {
        var pattern = "^((http://localhost/?)|(https://)|/?)[-a-zA-Z0-9@:%._\\+~#=]{1,256}[a-zA-Z0-9()]{1,6}(\\/[A-Za-z0-9]*)*(\\/[a-z0-9]+([A-Z]?[a-z0-9]+)*)*\\/v[0-9]+$";

        var url = "/api/v3";
        assertTrue(url.matches(pattern));
        url = "myserver/api/v3";
        assertTrue(url.matches(pattern), url);
        url = "/myserver/api/v3";
        assertTrue(url.matches(pattern), url);
        url = "https://myorg/api/v3";
        assertTrue(url.matches(pattern), url);
        url = "http://localhost/api/v3";
        assertTrue(url.matches(pattern), url);
        url = "http://myorg/api/v3";
        assertFalse(url.matches(pattern), url);
        url = "https://myorg/api";
        assertFalse(url.matches(pattern), url);
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

        var paths = ApiFunctions.buildAllPathWithLineRange(openApi, oas);
        assertNotNull(paths);
        paths.forEach(p-> assertTrue(p.getEnd() > p.getStart()));
        paths.forEach(p-> log.debug(p.toString()));
    }

    @Test
    void isInPathList() throws IOException {
        var oas = new OpenApiViolationAggregator();
        var file = new File(getClass().getResource("../rules/swagger_bad.yaml").getFile());
        var openApi = ApiFunctions.buildOpenApiSpecification(file, oas);

        var paths = ApiFunctions.buildAllPathWithLineRange(openApi, oas);
        assertTrue(ApiFunctions.isInPathList(paths, "/health", 2070));
    }

    @Test
    void getItems() throws IOException {
        var api = getOpenApi();
        var paths = ApiFunctions.getPaths(api);
        assertNotNull(paths);
        var pathItem = paths.getPathItems().get("/entity/contacts");
        var apiResponse =  pathItem.getGET().getResponses().getAPIResponses().get("200");

        apiResponse.getContent().getMediaTypes().values().forEach(mediaType -> {
            log.debug("items: {}", mediaType.getSchema().getItems());
            log.debug("properties: {}", mediaType.getSchema().getProperties());

            var ref = mediaType.getSchema().getRef();
            log.debug("ref: {}", ref);
            var schema = api.getComponents().getSchemas().get("ContactsCollection");
            log.debug("schema.getProperties()..containsKey('items'): {}", schema.getProperties().containsKey("items"));
        });
    }

    @Test
    void testIsReturnCollection() throws IOException {
        var api = getOpenApi();
        var pathItem = ApiFunctions.getPaths(api).getPathItems().get("/entity/contacts");
        var isReturnCollection = ApiFunctions.isReturnCollection(api, pathItem);

        assertTrue(isReturnCollection);
    }

    @Test
    void getReturnCollectionPathKey() throws IOException {
        var paths = ApiFunctions.getReturnCollectionPathKey(getOpenApi());
        assertNotNull(paths);
        assertEquals(11, paths.size());
    }

    @Test
    void getCollectionPathItems() throws IOException {
        Map<String, PathItem> items = ApiFunctions.getCollectionPathItems(getOpenApi());
        assertEquals(11, items.size());
    }

    @Test
    void isCompatibleMediaType() {
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        mediaTypes.add(MediaType.parseMediaType("multipart/*"));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/problem+json", mediaTypes));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/json", mediaTypes));
        assertFalse(ApiFunctions.isMediaTypeIncluded("application/problem+xml", mediaTypes));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/*", mediaTypes));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/form-data", mediaTypes));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/chunked", mediaTypes));
        List<MediaType> all = new ArrayList<>();
        all.add(MediaType.ALL);
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/problem+json", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/json", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/problem+xml", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/*", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/form-data", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/chunked", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("*/*", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/*", all));

        assertFalse(ApiFunctions.isMediaTypeIncluded("*/*", List.of(MediaType.APPLICATION_JSON)));

    }
}