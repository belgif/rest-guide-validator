package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class LineNumberTest {

    @Test
    public void testGetOperationId() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var operations = result.getOperations();
        var operation = operations.stream().filter(op -> op.getParent().getIdentifier().equals("/logos") && op.getMethod().equals(PathItem.HttpMethod.GET)).findAny();
        assertTrue(operation.isPresent());

        assertEquals(43, operation.get().getLineNumber().getLineNumber());
        assertEquals("openapi.yaml", operation.get().getLineNumber().getFileName());
    }

    @Test
    public void testProblemSchema() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getSchemas();
        var def = defs.stream().filter(definition -> "Problem".equals(definition.getIdentifier())).findAny();
        assertTrue(def.isPresent());

        assertEquals("problem-v1.yaml", def.get().getLineNumber().getFileName());
        assertEquals(16, def.get().getLineNumber().getLineNumber());
    }

    @Test
    public void testNestedSchema() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getSchemas();
        var def = defs.stream().filter(definition -> "arrayItemOne".equals(definition.getIdentifier())).findAny();
        assertTrue(def.isPresent());

        assertEquals("array.yaml", def.get().getLineNumber().getFileName());
        assertEquals(8, def.get().getLineNumber().getLineNumber());
    }

    @Test
    public void testMediaType() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getMediaTypes();
        var def = defs.stream().filter(definition -> "/paths/~1logos/post/requestBody/content/multipart~1form-data".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("openapi.yaml", def.get().getLineNumber().getFileName());
        assertEquals(32, def.get().getLineNumber().getLineNumber());
    }

    @Test
    public void testRequestBody() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getRequestBodies();
        var def = defs.stream().filter(definition -> "/paths/~1logos/post/requestBody".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("openapi.yaml", def.get().getLineNumber().getFileName());
        assertEquals(29, def.get().getLineNumber().getLineNumber());
    }

    @Test
    public void testParameters() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getParameters();
        var def = defs.stream().filter(definition -> "/paths/~1logos/get/parameters/0".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("openapi.yaml", def.get().getLineNumber().getFileName());
        assertEquals(47, def.get().getLineNumber().getLineNumber());
    }

    @Test
    public void testServers() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getServers();
        var def = defs.stream().filter(definition -> "/servers/0".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("openapi.yaml", def.get().getLineNumber().getFileName());
        assertEquals(5, def.get().getLineNumber().getLineNumber());
    }

    @Test
    public void testMultipleServers() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/multipleServers.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getServers();
        var def = defs.stream().filter(definition -> "/servers/0".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("multipleServers.yaml", def.get().getLineNumber().getFileName());
        assertEquals(3, def.get().getLineNumber().getLineNumber());

        def = defs.stream().filter(definition -> "/servers/1".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("multipleServers.yaml", def.get().getLineNumber().getFileName());
        assertEquals(8, def.get().getLineNumber().getLineNumber());

        def = defs.stream().filter(definition -> "/servers/2".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("multipleServers.yaml", def.get().getLineNumber().getFileName());
        assertEquals(13, def.get().getLineNumber().getLineNumber());

        def = defs.stream().filter(definition -> "/servers/3".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("multipleServers.yaml", def.get().getLineNumber().getFileName());
        assertEquals(18, def.get().getLineNumber().getLineNumber());

        def = defs.stream().filter(definition -> "/servers/4".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("multipleServers.yaml", def.get().getLineNumber().getFileName());
        assertEquals(19, def.get().getLineNumber().getLineNumber());

    }

    @Test
    public void testNestedArrays() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/lineNumberTests/nestedArrays.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getParameters();
        var def = defs.stream().filter(definition -> "/paths/~1everythingIsWrongHere/get/parameters/2".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("nestedArrays.yaml", def.get().getLineNumber().getFileName());
        assertEquals(30, def.get().getLineNumber().getLineNumber());
    }

    @Test
    public void testEnums() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/lineNumberTests/enumCounter.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getSchemas();
        var def = defs.stream().filter(definition -> "/paths/~1myFirstPath/get/parameters/0/schema".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("enumCounter.yaml", def.get().getLineNumber().getFileName());
        assertEquals(14, def.get().getLineNumber().getLineNumber());
    }

    @Test
    public void testResponse() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/lineNumberTests/responseHeader.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getResponses();
        var def = defs.stream().filter(definition -> "/paths/~1everythingIsWrongHere/get/responses/200".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("responseHeader.yaml", def.get().getLineNumber().getFileName());
        assertEquals(21, def.get().getLineNumber().getLineNumber());
    }

    @Test
    public void testMediaTypes() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/lineNumberTests/mediaTypeDefinitions.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getMediaTypes();
        var def = defs.stream().filter(definition -> "/components/responses/ProblemResponse/content/application~1problem+json".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("mediaTypeDefinitions.yaml", def.get().getLineNumber().getFileName());
        assertEquals(91, def.get().getLineNumber().getLineNumber());

        def = defs.stream().filter(definition -> "/paths/~1faultyProblemResponse/get/responses/default/content/application~1problem+json".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("mediaTypeDefinitions.yaml", def.get().getLineNumber().getFileName());
        assertEquals(32, def.get().getLineNumber().getLineNumber());

        def = defs.stream().filter(definition -> "/paths/~1faultyInlineResponse/get/responses/200/content/application~1json".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        assertEquals("mediaTypeDefinitions.yaml", def.get().getLineNumber().getFileName());
        assertEquals(10, def.get().getLineNumber().getLineNumber());

    }

    @Test
    public void testJsonPointerOnStringValue() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("rules/lineNumberTests/nestedArrays.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getPathDefinitions();
        var def = defs.stream().filter(definition -> "/paths/~1everythingIsWrongHere".equals(definition.getJsonPointer().toString())).findAny();
        assertTrue(def.isPresent());

        var schemaDefinition = new SchemaDefinition(def.get().getModel().getGET().getParameters().get(1).getSchema(), def.get(), new JsonPointer("/get/parameters/1/schema/items/enum/1"));

        assertEquals("nestedArrays.yaml", schemaDefinition.getLineNumber().getFileName());
        assertEquals(27, schemaDefinition.getLineNumber().getLineNumber());
    }

}
