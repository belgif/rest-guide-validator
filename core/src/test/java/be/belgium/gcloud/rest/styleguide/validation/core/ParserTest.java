package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.core.model.SchemaDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class ParserTest {

    private OpenAPI getOpenApi() throws IOException {
        var file = new File(getClass().getResource("../rules/swagger_bad.yaml").getFile());
        return Parser.buildOpenApiSpecification(file);
    }

    @Test
    void buildOpenApiSpecification() throws IOException {
        assertNotNull(getOpenApi());
    }

    @Test
    void buildUgly() throws IOException {
        var file = new File(getClass().getResource("../rules/ugly.json").getFile());
        var openApi = Parser.buildOpenApiSpecification(file);
        assertNotNull(openApi);
    }

    @Test
    void getAllPathWithLineRange() {
        var file = new File(getClass().getResource("../rules/swagger_bad.yaml").getFile());
        var oas = new OpenApiViolationAggregator();
        var result = new Parser(file).parse(oas);
        var paths = result.getPaths();

        paths.forEach(p -> assertTrue(p.getEnd() > p.getStart()));
        paths.forEach(p -> log.debug(p.toString()));

    }

    @Test
    void isInPathList() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(getClass().getResource("../rules/swagger_bad.yaml").getFile());
        var result = new Parser(file).parse(oas);

        assertTrue(result.isInPathList(List.of("/health"), 2070));
    }

    @Test
    public void testConstructNestedSchema() {
        var oas = new OpenApiViolationAggregator();
        var openApiFile = new File(getClass().getResource("../rules/schemasOpenApi.yaml").getFile());
        var parserResult = new Parser(openApiFile).parse(oas);
        Optional<SchemaDefinition> jsonPointer = parserResult.getSchemas().stream().filter(def -> def.getJsonPointer().toString().endsWith("anyOf/1")).findAny();
        assertTrue(jsonPointer.isPresent());
    }

    @Test
    void getReferencedFilesTest() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);
        Set<String> files = parserResult.getSrc().keySet();

        assertEquals(6, files.size());
    }

    @Test
    void getReferencedFilesTestJson() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(getClass().getResource("../rules/ugly.json").getFile());
        var parserResult = new Parser(file).parse(oas);
        Set<String> files = parserResult.getSrc().keySet();

        assertEquals(1, files.size());
    }

}
