package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.Line;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.fail;

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