package io.github.belgif.rest.guide.validator.output;

import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.fail;

class JUnitOutputProcessorTest {

    @Test
    void processByRule() throws IOException {
        var processor = new JUnitOutputProcessor(OutputGroupBy.RULE, Files.createTempDirectory(Paths.get("target"), "tmpDirPrefix").toFile());
        try {
            processor.process(getViolationAggregator());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    void processByFile() throws IOException {
        var processor = new JUnitOutputProcessor(OutputGroupBy.FILE, Files.createTempDirectory(Paths.get("target"), "tmpDirPrefix").toFile());
        try {
            processor.process(getViolationAggregator());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private ViolationReport getViolationAggregator() {
        var openApiViolationAggregator = new ViolationReport();
        openApiViolationAggregator.addViolation("Rule-test", "The rule message", new Line("", 155), "/MyPointer");
        openApiViolationAggregator.addViolation("Rule-test", "The rule message", new Line("", 170), "/MyPointer");
        openApiViolationAggregator.addViolation("Rule-test", "The rule message", new Line("", 180), "/MyPointer");

        return openApiViolationAggregator;
    }

}