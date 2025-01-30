package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.model.*;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ParserTest {

    @Test
    void buildOpenApiSpecification() throws IOException {
        var file = new File(getClass().getResource("../rules/logo.yaml").getFile());
        assertNotNull( Parser.buildOpenApiSpecification(file));
    }

    @Test
    void buildUgly() throws IOException {
        var file = new File(getClass().getResource("../rules/ugly.json").getFile());
        var openApi = Parser.buildOpenApiSpecification(file);
        assertNotNull(openApi);
        assertNotNull(openApi.getPaths());
    }

    @Test
    void getAllPathWithLineRange() {
        var file = new File(getClass().getResource("../rules/schemasOpenApi.yaml").getFile());
        var oas = new ViolationReport();
        var result = new Parser(file).parse(oas);
        var paths = result.getPaths();

        paths.forEach(p -> assertTrue(p.getEnd() > p.getStart()));
        paths.forEach(p -> log.debug(p.toString()));

    }

    @Test
    void isInPathList() {
        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/schemasOpenApi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        assertTrue(result.isInPathList(List.of("/everythingIsGoodHere"), 8));
    }

    @Test
    void testConstructNestedSchema() {
        var oas = new ViolationReport();
        var openApiFile = new File(getClass().getResource("../rules/schemasOpenApi.yaml").getFile());
        var parserResult = new Parser(openApiFile).parse(oas);
        var jsonPointer = parserResult.getSchemas().stream().filter(def -> def.getJsonPointer().toString().endsWith("anyOf/1")).findAny();
        assertTrue(jsonPointer.isPresent());
    }

    @Test
    void getReferencedFilesTest() {
        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);
        var files = parserResult.getSrc().keySet();

        assertEquals(6, files.size());
    }

    @Test
    void getReferencedFilesTestJson() {
        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/ugly.json").getFile());
        var parserResult = new Parser(file).parse(oas);
        var files = parserResult.getSrc().keySet();

        assertEquals(1, files.size());
    }

    @Test
    void testIgnoreExtensionIsParsed() {
        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/ignoreTests/ignoreTest.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getSchemas();
        var def = defs.stream().filter(definition -> "/paths/~1myFirstPath/get/parameters/0/schema".equals(definition.getJsonPointer().toString())).findAny();

        assertTrue(def.isPresent());
        assertFalse(def.get().getIgnoredRules().isEmpty());
        assertEquals(1, def.get().getIgnoredRules().size());
        assertTrue(def.get().getIgnoredRules().containsKey("cod-design"));
        assertEquals("Test reason", def.get().getIgnoredRules().get("cod-design"));
    }

    @Test
    void testInvalidRef() {
        var logger = (Logger) LoggerFactory.getLogger(OpenApiDefinition.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(listAppender);

        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/invalidRefs.yaml").getFile());

        listAppender.start();
        var ex = assertThrows(RuntimeException.class, () -> new Parser(file).parse(oas));
        var errorMessage = "Input file is not a valid OpenAPI document. Compliance to the REST style guidelines could not be verified.";

        assertEquals(errorMessage, ex.getMessage());
        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("/paths/logos/get/responses/200")));
        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("/paths/logos/get/parameters/1")));
        assertEquals(3, listAppender.list.size());
    }

    @Test
    void testValidButNonExistingRefResolve() {
        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/nonExistingRef.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getPathDefinitions();
        var def = defs.stream().filter(defenition -> "/paths/~1doesNotExist".equals(defenition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());
        var response = def.get().getModel().getGET().getResponses().getAPIResponse("200");
        assertNotNull(response);

        var ex = assertThrows(RuntimeException.class, () -> result.resolve(response));
        var errorMessage = "[Internal error] Could not find match of #/components/responses/doesNotExist";
        assertEquals(errorMessage, ex.getMessage());
    }

    @Test
    void testInvalidRefSwagger() {
        var logger = (Logger) LoggerFactory.getLogger(OpenApiDefinition.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(listAppender);

        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/invalidRefSwagger.yaml").getFile());

        listAppender.start();
        var ex = assertThrows(RuntimeException.class, () -> new Parser(file).parse(oas));
        var errorMessage = "Input file is not a valid OpenAPI document. Compliance to the REST style guidelines could not be verified.";
        assertEquals(errorMessage, ex.getMessage());

        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("/paths/userInfo/get/responses/200")));
        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("/paths/userInfo/get/responses/400/content/application/json/schema")));
        assertEquals(3, listAppender.list.size());
    }

    @Test
    void testInValidNonExistingRef() {
        var logger = (Logger) LoggerFactory.getLogger(OpenApiDefinition.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(listAppender);

        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/headersTest.yaml").getFile());
        var result = new Parser(file).parse(oas);

        result.oasVersion = 2;
        var parent = result.getResponses().stream().findAny();
        assertTrue(parent.isPresent());

        var headerOpt = result.getHeaders().stream().filter(header -> "#/components/headers/My-First-Response-Header".equals(header.getModel().getRef())).findFirst();
        assertTrue(headerOpt.isPresent());
        var headerModel = headerOpt.get().getModel();
        headerModel.setRef("#/blabla");

        listAppender.start();
        new ResponseHeaderDefinition(headerModel, parent.get(), "myFalseHeader");
        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("'#/blabla' is not of correct type")));
    }

    @Test
    public void testRefsToExamples() {
        var logger = (Logger) LoggerFactory.getLogger(OpenApiDefinition.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(listAppender);

        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/examplesRefs.yaml").getFile());

        listAppender.start();
        var ex = assertThrows(RuntimeException.class, () -> new Parser(file).parse(oas));
        var errorMessage = "Input file is not a valid OpenAPI document. Compliance to the REST style guidelines could not be verified.";
        assertEquals(errorMessage, ex.getMessage());

        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("#/components/schemas/MySchema")));
        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("/components/examples")));
        assertEquals(1, listAppender.list.size());
    }

    @Test
    void testRefsToExternalSchemasWithSameName() {
        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/referencedFiles/exampleRef.yaml").getFile());

        Parser.ParserResult result = new Parser(file).parse(oas);
        assertEquals(2, result.getExamples().size());
        assertEquals(1, result.getExamples().stream().filter(def -> def.getDefinitionType().equals(OpenApiDefinition.DefinitionType.INLINE)).collect(Collectors.toSet()).size());
        ExampleDefinition exampleDefinition = result.getExamples().stream().filter(def -> def.getDefinitionType().equals(OpenApiDefinition.DefinitionType.INLINE)).findFirst().orElse(null);
        assertNotNull(exampleDefinition);
        ExampleDefinition topLevelExampleDefinition = result.getExamples().stream().filter(def -> def.getDefinitionType().equals(OpenApiDefinition.DefinitionType.TOP_LEVEL)).findFirst().orElse(null);
        assertNotNull(topLevelExampleDefinition);

        for (int i=0; i < 100; i++) {
            assertEquals(topLevelExampleDefinition, result.resolve(exampleDefinition.getModel()));
        }
    }

    @Test
    void testNonExistingSecuritySchemes() {
        var logger = (Logger) LoggerFactory.getLogger(Parser.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(listAppender);

        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/nonExistingSecuritySchemes.yaml").getFile());

        listAppender.start();
        var ex = assertThrows(RuntimeException.class, () -> new Parser(file).parse(oas));
        var errorMessage = "Input file is not a valid OpenAPI document. Compliance to the REST style guidelines could not be verified.";
        assertEquals(errorMessage, ex.getMessage());

        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("/paths/myFirstPath/get/security/0")));
        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("/security/0")));
        assertEquals(3, listAppender.list.size());
    }

    @Test
    void testNonExistingSecuritySchemesSwagger() {
        var logger = (Logger) LoggerFactory.getLogger(Parser.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(listAppender);

        var oas = new ViolationReport();
        var file = new File(getClass().getResource("../rules/nonExistingSecuritySchemesSwagger.yaml").getFile());

        listAppender.start();
        var ex = assertThrows(RuntimeException.class, () -> new Parser(file).parse(oas));
        var errorMessage = "Input file is not a valid OpenAPI document. Compliance to the REST style guidelines could not be verified.";
        assertEquals(errorMessage, ex.getMessage());

        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("/paths/myFirstPath/get/security/0")));
        assertTrue(listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("/security/0")));
        assertEquals(3, listAppender.list.size());
    }

    @Test
    void testNonValidExampleIsParsed() {
        var oas = new ViolationReport();
        var file = new File(Objects.requireNonNull(getClass().getResource("../rules/wrongExampleFormat.yaml")).getFile());

        Parser.ParserResult result = new Parser(file).parse(oas);
        assertEquals(2, result.getExamples().size());
    }

    @Test
    void testRefOnly() {
        var oas = new ViolationReport();
        var file = new File(Objects.requireNonNull(getClass().getResource("../rules/referenceOnly/refOnly.yaml")).getFile());

        Parser.ParserResult result = new Parser(file).parse(oas);
        assertFalse(result.getPathDefinitions().stream().findFirst().get().isDirectPath());
    }

    @Test
    void testRefOnlyFalse() {
        var oas = new ViolationReport();
        var file = new File(Objects.requireNonNull(getClass().getResource("../rules/referenceOnly/refOnlyFalse.yaml")).getFile());

        Parser.ParserResult result = new Parser(file).parse(oas);
        assertEquals(1, result.getServers().size());
        assertTrue(result.getPathDefinitions().stream().findFirst().get().isDirectPath());
    }

    @Test
    void testRefOnlyRandomString() {
        var oas = new ViolationReport();
        var file = new File(Objects.requireNonNull(getClass().getResource("../rules/referenceOnly/refOnlyRandomString.yaml")).getFile());

        Parser.ParserResult result = new Parser(file).parse(oas);
        assertEquals(1, result.getServers().size());
        assertTrue(result.getPathDefinitions().stream().findFirst().get().isDirectPath());
    }
}
