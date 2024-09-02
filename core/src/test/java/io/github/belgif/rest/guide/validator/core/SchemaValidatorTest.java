package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.model.ExampleDefinition;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import io.github.belgif.rest.guide.validator.core.util.SchemaValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorTest {
    @Test
    void testCircularDependency() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/circularDependency.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples();
        var example = examples.stream().findAny();
        assertTrue(example.isPresent());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(example.get()));
    }

    @Test
    void testSameComponentNameInRef() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/sameName.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples().stream().filter(exampleDefinition -> exampleDefinition.getDefinitionType().equals(OpenApiDefinition.DefinitionType.INLINE)).collect(Collectors.toSet());

        assertDoesNotThrow(()->examples.stream().map(SchemaValidator::getExampleViolations));
    }

    @Test
    void testValidationOnDeeperSchemas() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/parameter.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples().stream().filter(exampleDefinition -> exampleDefinition.getJsonPointer().getJsonPointer().equals("/paths/~1encryptionKeys~1{id}/parameters/0/example")).collect(Collectors.toList());

        assertEquals(1, examples.size());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
        assertEquals(1, SchemaValidator.getExampleViolations(examples.get(0)).lines().count());
    }

}
