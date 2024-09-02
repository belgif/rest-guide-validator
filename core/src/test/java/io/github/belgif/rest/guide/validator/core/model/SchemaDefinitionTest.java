package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaDefinitionTest {

    @Test
    void testHighLevelSchema() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("../../rules/openapiDefinitionTestFiles/highLevelSchemaTest.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<SchemaDefinition> defs = result.getSchemas();
        var def = defs.stream().filter(definition -> "/components/schemas/MySchema/properties/hello".equals(definition.getJsonPointer().toPrettyString())).findAny();
        assertTrue(def.isPresent());
        assertTrue(def.get().isHighLevelSchema());
    }

    @Test
    void testComplexHighLevelSchema() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("../../rules/openapiDefinitionTestFiles/highLevelSchemaTest.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<SchemaDefinition> defs = result.getSchemas();
        var def = defs.stream().filter(definition -> "/components/schemas/Colors/allOf/1".equals(definition.getJsonPointer().toPrettyString())).findAny();
        assertTrue(def.isPresent());
        assertFalse(def.get().isHighLevelSchema());
    }

}
