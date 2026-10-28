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
    void testDefaultsEnumsAndActionableLocations() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("schemaValidationRegression.yaml").getFile());
        var result = new Parser(file).parse(oas);

        SchemaDefinition acceptedDefault = result.getSchemas().stream()
                .filter(schema -> "AcceptedDefault".equals(schema.getIdentifier()))
                .findFirst()
                .orElseThrow();
        assertTrue(SchemaValidator.getDefaultValueViolations(acceptedDefault).isEmpty());

        SchemaDefinition invalidDefault = result.getSchemas().stream()
                .filter(schema -> "InvalidDefault".equals(schema.getIdentifier()))
                .findFirst()
                .orElseThrow();
        assertTrue(SchemaValidator.getDefaultValueViolations(invalidDefault).orElseThrow().contains("InvalidDefault"));

        SchemaDefinition enumSchema = result.getSchemas().stream()
                .filter(schema -> "EnumValues".equals(schema.getIdentifier()))
                .findFirst()
                .orElseThrow();
        var enumViolations = SchemaValidator.getEnumViolations(enumSchema);
        assertEquals(1, enumViolations.size());
        assertTrue(enumViolations.iterator().next().getKey().equals("42"));

        ExampleDefinition invalidExample = result.getExamples().stream()
                .filter(example -> example.getJsonPointer().toString().contains("ExampleLocation"))
                .findFirst()
                .orElseThrow();
        String exampleViolation = SchemaValidator.getExampleViolations(invalidExample).orElseThrow();
        assertTrue(exampleViolation.contains("Instance /member"));
        assertTrue(exampleViolation.contains("schemaValidationRegression.yaml#/components/schemas/ExampleLocation"));

        var externalResult = new Parser(new File(this.getClass()
                .getResource("../rules/referencedFiles/openapi.yaml").getFile())).parse(new ViolationReport());
        SchemaDefinition externalDefault = externalResult.getSchemas().stream()
                .filter(schema -> schema.getOpenApiFile().getName().equals("problem-v1.yaml")
                        && schema.getJsonPointer().toString().endsWith("/properties/type"))
                .findFirst()
                .orElseThrow();
        assertTrue(SchemaValidator.getDefaultValueViolations(externalDefault).isEmpty());
    }

    @Test
    void testCircularDependency() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/circularDependency.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples();
        var example = examples.stream().findAny();
        assertTrue(example.isPresent());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(example.get()));
    }

    @Test
    void testSameComponentNameInRef() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/sameName.yaml").getFile());
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
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/parameter.yaml").getFile());
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
        var file = new File(this.getClass().getResource("../rules/discriminatorMappingsOnSchemaName.yaml").getFile());
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
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/refAsPropertyName.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().filter(def -> def.getPrintableJsonPointer().equals("/components/schemas/HealthStatus/example")).toList();

        assertEquals(1, examples.size());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
    }

    @Test
    void testRefAsPropertyNameOfDiscriminator() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/refAsPropertyNameInDiscriminator.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(1, examples.size());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
    }

    @Test
    void testUriReference() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/uriReference.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(1, examples.size());
        Optional<String> uriReferenceViolation = assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
        assertTrue(uriReferenceViolation.isEmpty());
    }

    @Test
    void testUri() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/uri.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(1, examples.size());
        Optional<String> uriViolation = assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
        assertTrue(uriViolation.isPresent());
    }

    @Test
    void testUriWithTrailingForwardSlashDoesNotAliasReferencedFile() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/uriWithTrailingForwardSlash.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(2, examples.size());
        assertThrows(IllegalStateException.class, () -> examples.forEach(SchemaValidator::getExampleViolations));
    }

}
