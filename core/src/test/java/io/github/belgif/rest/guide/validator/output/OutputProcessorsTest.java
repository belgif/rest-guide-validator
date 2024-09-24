package io.github.belgif.rest.guide.validator.output;

import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.core.ViolationLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

class OutputProcessorsTest {

    @Test
    void processConsoleByRule() {
        var consoleOutputProcessor = new ConsoleOutputProcessor(OutputGroupBy.RULE);
        try {
            consoleOutputProcessor.process(getViolationAggregator());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void processConsoleByFile() {
        var consoleOutputProcessor = new ConsoleOutputProcessor(OutputGroupBy.FILE);
        try {
            consoleOutputProcessor.process(getViolationAggregator());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void processLogByRule() {
        var log4JOutputProcessor = new Log4JOutputProcessor(OutputGroupBy.RULE);
        try {
            log4JOutputProcessor.process(getViolationAggregator());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void processLogByFile() {
        var log4JOutputProcessor = new Log4JOutputProcessor(OutputGroupBy.FILE);
        try {
            log4JOutputProcessor.process(getViolationAggregator());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private OpenApiViolationAggregator getViolationAggregator() throws IOException {
        var openApiViolationAggregator = new OpenApiViolationAggregator();
        openApiViolationAggregator.setTime(0.55f);
        openApiViolationAggregator.setRuleNumber(5);
        openApiViolationAggregator.addViolation("Rule-test", "The rume message", new Line("file1", 155), "/MyPointer");
        openApiViolationAggregator.addViolation("Rule-test", "The rume message", new Line("file2", 150), "/MyPointer2");
        openApiViolationAggregator.addViolation("Second-Rule", "My message", new Line("file1", 155), "/MyPointer");
        var ignoredViolation = new Violation("Rule-test", "This message is IGNORED", ViolationLevel.IGNORED, new Line("file1", 120), "myOtherPointer");
        openApiViolationAggregator.addViolation(ignoredViolation);

        return openApiViolationAggregator;
    }
}