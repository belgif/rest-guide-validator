package io.github.belgif.rest.guide.validator.runner.output;

import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.core.ViolationLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class OutputProcessorsTest {

    @Test
    void processConsoleByRule() {
        var consoleOutputProcessor = new ConsoleOutputProcessor(OutputGroupBy.RULE);
        try {
            consoleOutputProcessor.process(getViolationAggregator());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    void processConsoleByFile() {
        var consoleOutputProcessor = new ConsoleOutputProcessor(OutputGroupBy.FILE);
        try {
            consoleOutputProcessor.process(getViolationAggregator());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    void processLogByRule() {
        var loggerOutputProcessor = new LoggerOutputProcessor(OutputGroupBy.RULE);
        try {
            loggerOutputProcessor.process(getViolationAggregator());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    void processLogByFile() {
        var loggerOutputProcessor = new LoggerOutputProcessor(OutputGroupBy.FILE);
        try {
            loggerOutputProcessor.process(getViolationAggregator());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private ViolationReport getViolationAggregator() {
        var openApiViolationAggregator = new ViolationReport();
        openApiViolationAggregator.addViolation("Rule-test", "The rume message", new Line("file1", 155), "/MyPointer");
        openApiViolationAggregator.addViolation("Rule-test", "The rume message", new Line("file2", 150), "/MyPointer2");
        openApiViolationAggregator.addViolation("Second-Rule", "My message", new Line("file1", 155), "/MyPointer");
        var ignoredViolation = new Violation("Rule-test", "This message is IGNORED", ViolationLevel.IGNORED, new Line("file1", 120), "myOtherPointer");
        openApiViolationAggregator.addViolation(ignoredViolation);

        return openApiViolationAggregator;
    }
}