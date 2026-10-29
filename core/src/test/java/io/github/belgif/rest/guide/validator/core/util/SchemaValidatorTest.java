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

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void testCircularExternalReferenceAppliesReferencedConstraints() {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/circularDependency.yaml", "Logos");
        Set<String> violations = validateExample(schema);
        assertTrue(violations.stream().anyMatch(violation -> violation.contains("mediaType")));
    }

    @Test
    void testExternalReferenceIdentityUsesReferencedFile() {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/sameName.yaml", "Employer");
        Set<String> violations = validateExample(schema);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(violation -> violation.contains("referencedFile.yaml")));
        assertTrue(violations.stream().anyMatch(violation -> violation.contains("/employer")));
    }

    @Test
    void testDeeperParameterSchemaRejectsInvalidIdentifier() throws IOException {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/parameter.yaml", "Identifier");
        JsonNode instance = OBJECT_MAPPER.readTree("\"91504de4-7f27-491e-96dc-5c8fefc4814fab\"");

        Set<String> violations = SchemaValidator.validateSchema(instance, schema);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().contains("pattern"));
    }

    @Test
    void testRefPropertyNameIsValid() {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/refAsPropertyName.yaml", "HealthStatus");
        assertTrue(validateExample(schema).isEmpty());
    }

    @Test
    void testRefPropertyNameInDiscriminatorIsValid() {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/refAsPropertyNameInDiscriminator.yaml", "MyFirstSchema");
        assertTrue(validateExample(schema).isEmpty());
    }

    @Test
    void testUriReferenceFormatAcceptsRelativeReference() throws IOException {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/uriReference.yaml", "UriReference");
        JsonNode instance = OBJECT_MAPPER.readTree("{\"href\":\"./documents/123\"}");

        assertTrue(SchemaValidator.validateSchema(instance, schema).isEmpty());
    }

    @Test
    void testUriFormatRejectsRelativeReference() throws IOException {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/uri.yaml", "Uri");
        JsonNode instance = OBJECT_MAPPER.readTree("{\"href\":\"./documents/123\"}");

        assertFalse(SchemaValidator.validateSchema(instance, schema).isEmpty());
    }

    @Test
    void testValidationViolationsIncludeActionableLocations() throws IOException {
        SchemaDefinition schema = loadSchema("SchemaValidatorSupportTest/schemaValidationRegression.yaml", "ExampleLocation");
        JsonNode instance = OBJECT_MAPPER.readTree("{\"member\":42}");

        Set<String> violations = SchemaValidator.validateSchema(instance, schema);

        assertTrue(violations.stream().anyMatch(violation -> violation.contains("Value at /member")));
        assertTrue(violations.stream().anyMatch(violation -> violation.contains(
                "SchemaValidatorSupportTest/schemaValidationRegression.yaml#/components/schemas/ExampleLocation")));
    }

    @Test
    void testAllOfDiscriminatorChildSchemaResolution() throws Exception {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/discriminatorWithAllOf.yaml", "Pet");
        JsonNode instance = OBJECT_MAPPER.readTree("""
                {
                    "petType": "Dog",
                    "bark": 123
                }
                """
        );
        Set<String> violations = SchemaValidator.validateSchema(instance, schema);
        //networknt validator does not validate allOf-style discriminator mapping, so the example is valid
        assertTrue(violations.isEmpty());
    }

    @Test
    void testFailOnOverwrittenDiscriminatorMapping() throws Exception {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/overwrittenDiscriminatorMapping.yaml", "TestSchema");
        JsonNode instance = OBJECT_MAPPER.readTree("""
                {
                    "firstChildProperty": "test",
                    "objectType": "firstChild"
                }
                """
        );
        // New behavior: the networknt validator throws exception when a discriminator mapping is overwritten by schema composition
        assertThrows(IllegalStateException.class, () -> SchemaValidator.validateSchema(instance, schema));
    }

    @Test
    void testOneOfDiscriminatorValidCase() throws Exception {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/discriminatorWithOneOf.yaml", "TestSchema");
        JsonNode validInstance = OBJECT_MAPPER.readTree("""
                {
                    "firstChildProperty": "test",
                    "objectType": "firstChild"
                }
                """
        );
        Set<String> violations = SchemaValidator.validateSchema(validInstance, schema);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testOneOfDiscriminatorAllowsMatchingAnotherSubschema() throws Exception {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/discriminatorWithOneOf.yaml", "TestSchema");
        JsonNode instance = OBJECT_MAPPER.readTree("""
                {
                    "objectType": "firstChild",
                    "secondChildProperty": "test"
                }
                """
        );
        Set<String> violations = SchemaValidator.validateSchema(instance, schema);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testOneOfDiscriminatorRejectsUnmappedValue() throws Exception {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/discriminatorWithOneOf.yaml", "TestSchema");
        JsonNode instance = OBJECT_MAPPER.readTree("""
                {
                    "firstChildProperty": "test",
                    "objectType": "unknownChild"
                }
                """
        );

        Set<String> violations = SchemaValidator.validateSchema(instance, schema);
        assertEquals(1, violations.size());
    }


    @Test
    void testAllOfDiscriminatorInvalidDiscriminatorValue() throws Exception {
        SchemaDefinition schema = loadSchema("SchemaValidatorTest/discriminatorWithAllOf.yaml", "Pet");
        JsonNode instance = OBJECT_MAPPER.readTree("""
                {
                    "petType": "Doggy",
                    "bark": "woof"
                }
                """
        );
        Set<String> violations = SchemaValidator.validateSchema(instance, schema);
        //networknt validator does not validate allOf-style discriminator, so the example is valid
        assertTrue(violations.isEmpty());
    }

    private Set<String> validateExample(SchemaDefinition schema) {
        return SchemaValidator.validateSchema(schema.getJsonNode().get("example"), schema);
    }

    private SchemaDefinition loadSchema(String resourcePath, String identifier) {
        File file = new File(getClass().getResource(resourcePath).getFile());
        Parser.ParserResult parserResult = new Parser(file).parse(new ViolationReport());
        return parserResult.getSchemas().stream()
                .filter(schema -> identifier.equals(schema.getIdentifier()))
                .filter(schema -> file.getName().equals(schema.getOpenApiFile().getName()))
                .findFirst()
                .orElseThrow();
    }
}
