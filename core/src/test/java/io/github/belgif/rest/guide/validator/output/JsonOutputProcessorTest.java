package io.github.belgif.rest.guide.validator.output;

import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
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


    @Test
    void resolveFileWithDefaultValuesTest() {
        var outputProcessor = new JsonOutputProcessor(OutputGroupBy.RULE, null);
        outputProcessor.setOutputDirectory(new File("target")); // Default value
        assertEquals(new File("target/validationReport.json").getAbsoluteFile(), outputProcessor.resolveOutputFile());
    }

    @Test
    void resolveFileWithAbsoluteFileNameTest() {
        var outputProcessor = new JsonOutputProcessor(OutputGroupBy.RULE, new File("/myFolder/myCustomName.json"));
        assertEquals(new File("/myFolder/myCustomName.json").getAbsoluteFile(), outputProcessor.resolveOutputFile());
    }

    @Test
    void resolveFileWithRelativeFileNameTest() {
        var outputProcessor = new JsonOutputProcessor(OutputGroupBy.RULE, new File("myFolder/myCustomName.json"));
        var file = new File("myFolder/myCustomName.json").getAbsoluteFile();
        assertTrue(file.getPath().contains("rest-guide-validator" + File.separator + "core"));
        assertEquals(file, outputProcessor.resolveOutputFile());
    }

    private OpenApiViolationAggregator getViolationAggregator() {
        var openApiViolationAggregator = new OpenApiViolationAggregator();
        openApiViolationAggregator.setTime(0.55f);
        openApiViolationAggregator.setRuleNumber(3);
        openApiViolationAggregator.addViolation("Rule-test", "The rule message", new Line("", 155), "/MyPointer");
        openApiViolationAggregator.addViolation("Rule-test", "The rule message", new Line("", 170), "/MyPointer");
        openApiViolationAggregator.addViolation("Rule-test", "The rule message", new Line("", 180), "/MyPointer");

        return openApiViolationAggregator;
    }
}
