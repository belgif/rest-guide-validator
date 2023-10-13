package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.model.*;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class LineNumberTest {

    @Test
    public void testGetOperationId() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<OperationDefinition> operations = result.getOperations();
        var operation = operations.stream().filter(op -> op.getPath().equals("/logos") && op.getMethod().equals(PathItem.HttpMethod.GET)).findAny();
        assertTrue(operation.isPresent());

        // Assert on lineNumber of operationId
        assertEquals(45, operation.get().getLineNumber(oas).getLineNumber());
        assertEquals("openapi.yaml", operation.get().getLineNumber(oas).getFileName());
    }

    @Test
    public void testProblemSchema() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<SchemaDefinition> defs = result.getSchemas();
        var def = defs.stream().filter(definition -> "Problem".equals(definition.getIdentifier())).findAny();
        assertTrue(def.isPresent());

        assertEquals("problem-v1.yaml", def.get().getLineNumber(oas).getFileName());
        assertEquals(16, def.get().getLineNumber(oas).getLineNumber());
    }

    @Test
    public void testNestedSchema() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<SchemaDefinition> defs = result.getSchemas();
        var def = defs.stream().filter(definition -> "arrayItemOne".equals(definition.getIdentifier())).findAny();
        assertTrue(def.isPresent());

        assertEquals("array.yaml", def.get().getLineNumber(oas).getFileName());
        assertEquals(8, def.get().getLineNumber(oas).getLineNumber());
    }

    @Test
    public void testMediaType() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<MediaTypeDefinition> defs = result.getMediaTypes();
        var def = defs.stream().filter(definition -> "/paths//logos/post/requestBody/content/multipart/form-data".equals(definition.getJsonPointer())).findAny();
        assertTrue(def.isPresent());

        assertEquals("openapi.yaml", def.get().getLineNumber(oas).getFileName());
        assertEquals(32, def.get().getLineNumber(oas).getLineNumber());
    }

    @Test
    public void testRequestBody() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<RequestBodyDefinition> defs = result.getRequestBodies();
        var def = defs.stream().filter(definition -> "/paths//logos/post/requestBody".equals(definition.getJsonPointer())).findAny();
        assertTrue(def.isPresent());

        assertEquals("openapi.yaml", def.get().getLineNumber(oas).getFileName());
        assertEquals(29, def.get().getLineNumber(oas).getLineNumber());
    }

    @Test
    public void testParameters() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("rules/referencedFiles/openapi.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<ParameterDefinition> defs = result.getParameters();
        var def = defs.stream().filter(definition -> "/paths//logos/get/parameters/test".equals(definition.getJsonPointer())).findAny();
        assertTrue(def.isPresent());

        assertEquals("openapi.yaml", def.get().getLineNumber(oas).getFileName());
        assertEquals(47, def.get().getLineNumber(oas).getLineNumber());
    }
}
