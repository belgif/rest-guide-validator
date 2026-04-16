package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SchemaDefinitionTest {

    @Test
    void testHighLevelSchema() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../../rules/openapiDefinitionTestFiles/highLevelSchemaTest.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<SchemaDefinition> defs = result.getSchemas();
        var def = defs.stream().filter(definition -> "/components/schemas/MySchema/properties/hello".equals(definition.getJsonPointer().toPrettyString())).findAny();
        assertTrue(def.isPresent());
        assertTrue(def.get().isHighLevelSchema());
    }

    @Test
    void testComplexHighLevelSchema() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../../rules/openapiDefinitionTestFiles/highLevelSchemaTest.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<SchemaDefinition> defs = result.getSchemas();
        var def = defs.stream().filter(definition -> "/components/schemas/Colors/allOf/1".equals(definition.getJsonPointer().toPrettyString())).findAny();
        assertTrue(def.isPresent());
        assertFalse(def.get().isHighLevelSchema());
    }

    @Test
    void testIsInlineProperty() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../../rules/inlineProperty.yaml").getFile());
        var result = new Parser(file).parse(oas);
        Set<SchemaDefinition> defs = result.getSchemas();
        assertEquals(5, defs.size());
        assertEquals(1, defs.stream().filter(SchemaDefinition::isInlineProperty).collect(Collectors.toSet()).size());
    }

}
