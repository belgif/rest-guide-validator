package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaValidatorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void testCircularExternalReferenceAppliesReferencedConstraints() {
        SchemaDefinition schema = findSchema(parse(
                "SchemaValidatorTest/circularDependency.yaml"),
                "Logos", "circularDependency.yaml");
        Set<String> violations = validateExample(schema);

        assertTrue(violations.stream().anyMatch(violation -> violation.contains("mediaType")));
    }

    @Test
    void testExternalReferenceIdentityUsesReferencedFile() {
        SchemaDefinition schema = findSchema(parse(
                "SchemaValidatorTest/sameName.yaml"),
                "Employer", "sameName.yaml");
        Set<String> violations = validateExample(schema);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(violation -> violation.contains("referencedFile.yaml")));
        assertTrue(violations.stream().anyMatch(violation -> violation.contains("/employer")));
    }

    @Test
    void testDeeperParameterSchemaRejectsInvalidIdentifier() throws IOException {
        SchemaDefinition schema = findSchema(parse(
                "SchemaValidatorTest/parameter.yaml"),
                "Identifier", "parameter.yaml");
        JsonNode instance = OBJECT_MAPPER.readTree("\"91504de4-7f27-491e-96dc-5c8fefc4814fab\"");

        Set<String> violations = SchemaValidator.validateSchema(instance, schema);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().contains("pattern"));
    }

    @Test
    void testRefPropertyNameIsValid() {
        SchemaDefinition schema = findSchema(parse(
                "SchemaValidatorTest/refAsPropertyName.yaml"),
                "HealthStatus", "refAsPropertyName.yaml");

        assertTrue(validateExample(schema).isEmpty());
    }

    @Test
    void testRefPropertyNameInDiscriminatorIsValid() {
        SchemaDefinition schema = findSchema(parse(
                "SchemaValidatorTest/refAsPropertyNameInDiscriminator.yaml"),
                "MyFirstSchema", "refAsPropertyNameInDiscriminator.yaml");

        assertTrue(validateExample(schema).isEmpty());
    }

    @Test
    void testUriReferenceFormatAcceptsRelativeReference() throws IOException {
        SchemaDefinition schema = findSchema(parse(
                "SchemaValidatorTest/uriReference.yaml"),
                "UriReference", "uriReference.yaml");
        JsonNode instance = OBJECT_MAPPER.readTree("{\"href\":\"./documents/123\"}");

        assertTrue(SchemaValidator.validateSchema(instance, schema).isEmpty());
    }

    @Test
    void testUriFormatRejectsRelativeReference() throws IOException {
        SchemaDefinition schema = findSchema(parse(
                "SchemaValidatorTest/uri.yaml"),
                "Uri", "uri.yaml");
        JsonNode instance = OBJECT_MAPPER.readTree("{\"href\":\"./documents/123\"}");

        assertFalse(SchemaValidator.validateSchema(instance, schema).isEmpty());
    }

    @Test
    void testValidationViolationsIncludeActionableLocations() throws IOException {
        SchemaDefinition schema = findSchema(parse(
                "SchemaValidatorSupportTest/schemaValidationRegression.yaml"),
                "ExampleLocation", "schemaValidationRegression.yaml");
        JsonNode instance = OBJECT_MAPPER.readTree("{\"member\":42}");

        Set<String> violations = SchemaValidator.validateSchema(instance, schema);

        assertTrue(violations.stream().anyMatch(violation -> violation.contains("Value at /member")));
        assertTrue(violations.stream().anyMatch(violation -> violation.contains(
                "SchemaValidatorSupportTest/schemaValidationRegression.yaml#/components/schemas/ExampleLocation")));
    }

    private Set<String> validateExample(SchemaDefinition schema) {
        return SchemaValidator.validateSchema(schema.getJsonNode().get("example"), schema);
    }

    private SchemaDefinition findSchema(Parser.ParserResult result, String identifier, String fileName) {
        return result.getSchemas().stream()
                .filter(schema -> identifier.equals(schema.getIdentifier()))
                .filter(schema -> fileName.equals(schema.getOpenApiFile().getName()))
                .findFirst()
                .orElseThrow();
    }

    private Parser.ParserResult parse(String resourcePath) {
        File file = new File(getClass().getResource(resourcePath).getFile());
        return new Parser(file).parse(new ViolationReport());
    }
}
