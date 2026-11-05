package io.github.belgif.rest.guide.validator.core.util;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorSupportTest {
    @Test
    void testValidDefault() {
        SchemaDefinition schema = getRegressionSchema("AcceptedDefault");
        assertTrue(SchemaValidationSupport.getDefaultValueViolations(schema).isEmpty());
    }

    @Test
    void testInvalidDefault() {
        SchemaDefinition schema = getRegressionSchema("InvalidDefault");
        assertTrue(SchemaValidationSupport.getDefaultValueViolations(schema).orElseThrow().contains("InvalidDefault"));
    }

    @Test
    void testValidEnum() {
        SchemaDefinition schema = getRegressionSchema("ValidEnum");
        assertTrue(SchemaValidationSupport.getEnumViolations(schema).isEmpty());
    }

    @Test
    void testInvalidEnum() {
        SchemaDefinition schema = getRegressionSchema("InvalidEnum");
        var violations = SchemaValidationSupport.getEnumViolations(schema);

        assertEquals(1, violations.size());
        assertEquals("42", violations.iterator().next().getKey());
    }

    private SchemaDefinition getRegressionSchema(String identifier) {
        return parseRegressionDocument().getSchemas().stream()
                .filter(schema -> identifier.equals(schema.getIdentifier()))
                .findFirst()
                .orElseThrow();
    }

    private Parser.ParserResult parseRegressionDocument() {
        var file = new File(this.getClass().getResource("./SchemaValidatorSupportTest/schemaValidationRegression.yaml").getFile());
        return new Parser(file).parse(new ViolationReport());
    }

}
