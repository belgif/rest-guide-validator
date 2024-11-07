package io.github.belgif.rest.guide.validator.runner.output;

import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class JsonOutputProcessorTest {

    @Test
    void processJsonOutputTest() {
        try {
            var tempFile = new File(Files.createTempDirectory(Paths.get("target"), "tmpDirPrefix").toFile(), "myCustomFileName.json");
            assertFalse(tempFile.exists());
            var outputProcessor = new JsonOutputProcessor(OutputGroupBy.RULE, tempFile);
            outputProcessor.process(getViolationAggregator());
            assertTrue(tempFile.exists());
        } catch (IOException e) {
            fail();
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
