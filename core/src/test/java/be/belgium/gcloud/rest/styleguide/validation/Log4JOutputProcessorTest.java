package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

class Log4JOutputProcessorTest {

    @Test
    void process() {
        var log4JOutputProcessor = new Log4JOutputProcessor();
        try {
            log4JOutputProcessor.process(getViolationAggregator());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private OpenApiViolationAggregator getViolationAggregator() throws IOException {
        var openApiViolationAggregator = new OpenApiViolationAggregator();
        openApiViolationAggregator.setOpenApiFile(File.createTempFile("test", ""));
        openApiViolationAggregator.setTime(0.55f);
        openApiViolationAggregator.setRuleNumber(5);
        openApiViolationAggregator.addViolation("Rule-test", "The rume message", 155);

        return openApiViolationAggregator;
    }
}