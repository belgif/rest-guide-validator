package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import io.github.belgif.rest.guide.validator.core.util.SchemaValidator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorTest {
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

        assertDoesNotThrow(()->examples.stream().map(SchemaValidator::getExampleViolations));
    }

    @Test
    void testValidationOnDeeperSchemas() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/parameter.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples().stream().filter(exampleDefinition -> exampleDefinition.getJsonPointer().getJsonPointer().equals("/paths/~1encryptionKeys~1{id}/parameters/0/example")).collect(Collectors.toList());

        assertEquals(1, examples.size());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
        assertEquals(1, SchemaValidator.getExampleViolations(examples.get(0)).lines().count());
    }

    @Test
    void testDiscriminatorMappingOnSchemaName() {
        // In order to test the belgif fix for a bug in openapi4j where discriminator mapping did only work with $ref.
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/discriminatorMappingsOnSchemaName.yaml").getFile());
        var result = new Parser(file).parse(oas);

        for (SchemaDefinition schemaDefinition : result.getSchemas()) {
            if (schemaDefinition.getModel().getEnumeration() != null) {
                assertDoesNotThrow(() -> SchemaValidator.getEnumViolations(schemaDefinition));
            }
        }
    }

}
