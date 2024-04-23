package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.Line;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import org.junit.jupiter.api.Test;

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
        openApiViolationAggregator.setTime(0.55f);
        openApiViolationAggregator.setRuleNumber(5);
        openApiViolationAggregator.addViolation("Rule-test", "The rume message", new Line("", 155), "/MyPointer");

        return openApiViolationAggregator;
    }
}