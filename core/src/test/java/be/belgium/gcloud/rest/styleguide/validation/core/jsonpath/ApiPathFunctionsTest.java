package be.belgium.gcloud.rest.styleguide.validation.core.jsonpath;

import be.belgium.gcloud.rest.styleguide.validation.core.ApiFunctions;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.Criteria.where;
import static org.junit.jupiter.api.Assertions.*;

class ApiPathFunctionsTest {
    static String jsonString;

    @BeforeAll
    static void init() throws IOException {
        var file = new File(ApiPathFunctionsTest.class.getResource("../../rules/swagger_bad.yaml").getFile());
        var oas = new OpenApiViolationAggregator();
        ApiFunctions.buildOpenApiSpecification(file, oas);
        jsonString = getJsonString(oas);
    }
    private static String getJsonString(OpenApiViolationAggregator oas) throws JsonProcessingException {
        var yamlReader = new ObjectMapper(new YAMLFactory());
        var obj = yamlReader.readValue(oas.getSrc().stream().collect(Collectors.joining("\n")), Object.class);

        var jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }


    @Test
    void read() {
        assertNotNull(jsonString);
    }

    @Test
    void testRead() {
        assertNotNull(jsonString);
    }

    @Test
    void testPath() {
        assertNotNull(jsonString);
        var producesTpl = "$.paths[*].%s[?]['operationId','produces']";
        var document = Configuration.defaultConfiguration().jsonProvider().parse(jsonString);

        var produceFilter = Filter.filter(
                where("produces").noneof(ApiPathFunctions.PRODUCES_MEDIATYPE));
        JSONArray arr = JsonPath.read(document, String.format(producesTpl, "get"), produceFilter);
        assertNotNull(arr);
        assertFalse(arr.isEmpty());

    }

    @Test
    void operationDontHaveProduce() {
        assertNotNull(jsonString);
        List<OperationData> list = ApiPathFunctions.operationDataDontHaveProduce(jsonString, PathItem.HttpMethod.GET, ApiPathFunctions.PRODUCES_MEDIATYPE);
        assertFalse(list.isEmpty());
    }

    @Test
    void operationDontHaveConsume() {
        assertNotNull(jsonString);
        List<OperationData> list = ApiPathFunctions.operationDataDontHaveConsume(jsonString, PathItem.HttpMethod.POST, ApiPathFunctions.CONSUMES_MEDIATYPE);
        assertFalse(list.isEmpty());
    }

    @Test
    void testOperationDontHaveProduce() {
        assertNotNull(jsonString);
        List<Object> list = ApiPathFunctions.operationIdDontHaveProduce(jsonString, PathItem.HttpMethod.GET);
        assertFalse(list.isEmpty());
    }

    @Test
    void testOperationDontHaveConsume() {
        assertNotNull(jsonString);
        List<Object> list = ApiPathFunctions.operationIdDontHaveConsume(jsonString, PathItem.HttpMethod.POST);
        assertFalse(list.isEmpty());
    }

    @Test
    void isSwaggerV2() throws IOException {
        assertNotNull(jsonString);
        assertTrue(ApiPathFunctions.isSwaggerV2(jsonString));

        var file = new File(ApiPathFunctionsTest.class.getResource("../../rules/petstore.json").getFile());
        var oas = new OpenApiViolationAggregator();
        ApiFunctions.buildOpenApiSpecification(file, oas);
        var jsonStringV3 = getJsonString(oas);

        assertNotNull(jsonStringV3);
        assertFalse(ApiPathFunctions.isSwaggerV2(jsonStringV3));
    }
}