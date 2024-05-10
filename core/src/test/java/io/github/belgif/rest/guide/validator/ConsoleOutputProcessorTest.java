package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

class ConsoleOutputProcessorTest {

    @Test
    void process() {
        var consoleOutputProcessor = new ConsoleOutputProcessor();
        try {
            consoleOutputProcessor.process(getViolationAggregator());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private OpenApiViolationAggregator getViolationAggregator() throws IOException {
        var openApiViolationAggregator = new OpenApiViolationAggregator();
        openApiViolationAggregator.setTime(0.55f);
        openApiViolationAggregator.setRuleNumber(5);
        openApiViolationAggregator.addViolation("Rule-test", "The rume message", new Line("", 155), "/MyPointer");

        return openApiViolationAggregator;
    }
}