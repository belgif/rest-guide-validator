package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.model.ExampleDefinition;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import io.github.belgif.rest.guide.validator.core.util.SchemaValidator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorTest {
    @Test
    void testValidDefault() {
        SchemaDefinition schema = getRegressionSchema("AcceptedDefault");
        assertTrue(SchemaValidator.getDefaultValueViolations(schema).isEmpty());
    }

    @Test
    void testInvalidDefault() {
        SchemaDefinition schema = getRegressionSchema("InvalidDefault");
        assertTrue(SchemaValidator.getDefaultValueViolations(schema).orElseThrow().contains("InvalidDefault"));
    }

    @Test
    void testValidEnum() {
        SchemaDefinition schema = getRegressionSchema("ValidEnum");
        assertTrue(SchemaValidator.getEnumViolations(schema).isEmpty());
    }

    @Test
    void testInvalidEnum() {
        SchemaDefinition schema = getRegressionSchema("InvalidEnum");
        var violations = SchemaValidator.getEnumViolations(schema);

        assertEquals(1, violations.size());
        assertEquals("42", violations.iterator().next().getKey());
    }

    @Test
    void testExampleViolationIncludesActionableLocations() {
        var result = parseRegressionDocument();
        ExampleDefinition invalidExample = result.getExamples().stream()
                .filter(example -> example.getJsonPointer().toString().contains("ExampleLocation"))
                .findFirst()
                .orElseThrow();

        String exampleViolation = SchemaValidator.getExampleViolations(invalidExample).orElseThrow();
        assertTrue(exampleViolation.contains("Value at /member"));
        assertTrue(exampleViolation.contains("validator/schemaValidationRegression.yaml#/components/schemas/ExampleLocation"));
    }

    private SchemaDefinition getRegressionSchema(String identifier) {
        return parseRegressionDocument().getSchemas().stream()
                .filter(schema -> identifier.equals(schema.getIdentifier()))
                .findFirst()
                .orElseThrow();
    }

    private Parser.ParserResult parseRegressionDocument() {
        var file = new File(this.getClass().getResource("./validator/schemaValidationRegression.yaml").getFile());
        return new Parser(file).parse(new ViolationReport());
    }

    @Test
    void testCircularDependency() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("./validator/circularDependency.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples();
        var example = examples.stream().findAny();
        assertTrue(example.isPresent());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(example.get()));
    }

    @Test
    void testSameComponentNameInRef() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("./validator/sameName.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples().stream().filter(exampleDefinition -> exampleDefinition.getDefinitionType().equals(OpenApiDefinition.DefinitionType.INLINE)).collect(Collectors.toSet());

        assertDoesNotThrow(() -> examples.forEach(SchemaValidator::getExampleViolations));
        String violations = SchemaValidator.getExampleViolations(examples.iterator().next()).orElseThrow();
        assertTrue(violations.contains("referencedFile.yaml"));
        assertTrue(violations.contains("/employer"));
    }

    @Test
    void testValidationOnDeeperSchemas() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("./validator/parameter.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples().stream().filter(exampleDefinition -> exampleDefinition.getJsonPointer().getJsonPointer().equals("/paths/~1encryptionKeys~1{id}/parameters/0/example")).collect(Collectors.toList());

        assertEquals(1, examples.size());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
        assertEquals(1, SchemaValidator.getExampleViolations(examples.get(0)).get().lines().count());
    }

    @Test
    void testDiscriminatorMappingOnSchemaName() {
        // Discriminator mapping validation is handled by the OpenAPI 3.0 schema dialect.
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("./validator/discriminatorMappingsOnSchemaName.yaml").getFile());
        var result = new Parser(file).parse(oas);

        for (SchemaDefinition schemaDefinition : result.getSchemas()) {
            if (schemaDefinition.getModel().getEnumeration() != null) {
                assertDoesNotThrow(() -> SchemaValidator.getEnumViolations(schemaDefinition));
            }
        }
    }

    @Test
    void testRefAsPropertyName() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("./validator/refAsPropertyName.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().filter(def -> def.getPrintableJsonPointer().equals("/components/schemas/HealthStatus/example")).toList();

        assertEquals(1, examples.size());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
    }

    @Test
    void testRefAsPropertyNameOfDiscriminator() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("./validator/refAsPropertyNameInDiscriminator.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(1, examples.size());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
    }

    @Test
    void testUriReference() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("./validator/uriReference.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(1, examples.size());
        Optional<String> uriReferenceViolation = assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
        assertTrue(uriReferenceViolation.isEmpty());
    }

    @Test
    void testUri() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("./validator/uri.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(1, examples.size());
        Optional<String> uriViolation = assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
        assertTrue(uriViolation.isPresent());
    }

}
