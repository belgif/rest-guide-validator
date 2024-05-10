package io.github.belgif.rest.styleguide.validation.core;

import io.github.belgif.rest.styleguide.validation.core.model.ExampleDefinition;
import io.github.belgif.rest.styleguide.validation.core.model.OpenApiDefinition;
import io.github.belgif.rest.styleguide.validation.core.parser.Parser;
import io.github.belgif.rest.styleguide.validation.core.util.ExampleValidator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ExampleValidatorTest {

    @Test
    public void testCircularDependency() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/circularDependency.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<ExampleDefinition> examples = result.getExamples();
        var example = examples.stream().findAny();
        assertTrue(example.isPresent());

        assertDoesNotThrow(() -> ExampleValidator.getExampleViolations(example.get()));
    }

    @Test
    public void testSameComponentNameInRef() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/sameName.yaml").getFile());
        var result = new Parser(file).parse(oas);

        Set<ExampleDefinition> examples = result.getExamples().stream().filter(exampleDefinition -> exampleDefinition.getDefinitionType().equals(OpenApiDefinition.DefinitionType.INLINE)).collect(Collectors.toSet());

        for (ExampleDefinition example : examples) {
            assertDoesNotThrow(() -> ExampleValidator.getExampleViolations(example));
        }
    }

}
