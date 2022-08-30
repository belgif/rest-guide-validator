package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Testcase;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JUnitOutputProcessor3Test {

    @Test
    void process() throws IOException {
        var processor = new JUnitOutputProcessor3();
        processor.setOutput(Files.createTempDirectory(Paths.get("target"), "tmpDirPrefix").toFile());
        try {
            processor.process(getViolationAggregator());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private OpenApiViolationAggregator getViolationAggregator() throws IOException {
        var openApiViolationAggregator = new OpenApiViolationAggregator();
        openApiViolationAggregator.setOpenApiFile(File.createTempFile("test", ""));
        openApiViolationAggregator.setTime(0.55f);
        openApiViolationAggregator.setRuleNumber(3);
        openApiViolationAggregator.addViolation("Rule-test", "The rule message", 155);
        openApiViolationAggregator.addViolation("Rule-test", "The rule message", 170);
        openApiViolationAggregator.addViolation("Rule-test", "The rule message", 180);

        return openApiViolationAggregator;
    }

}