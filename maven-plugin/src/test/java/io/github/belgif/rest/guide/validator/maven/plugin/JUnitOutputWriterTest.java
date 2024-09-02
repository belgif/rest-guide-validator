package io.github.belgif.rest.guide.validator.maven.plugin;

import io.github.belgif.rest.guide.validator.JUnitOutputProcessor;
import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.maven.junit.Error;
import io.github.belgif.rest.guide.validator.maven.junit.Failure;
import io.github.belgif.rest.guide.validator.maven.junit.Testcase;
import io.github.belgif.rest.guide.validator.maven.junit.Testsuite;
import io.github.belgif.rest.guide.validator.output.OutputGroupBy;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.fail;

class JUnitOutputWriterTest {
    @Test
    void writeEmpty() {
        try {
            var outputProcessor = new JUnitOutputProcessor(OutputGroupBy.RULE);
            outputProcessor.setOutput(Files.createTempDirectory("tmp").toFile());
            outputProcessor.write(new Testsuite());
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void write() {
        Testsuite testsuite = Testsuite.builder()
                .name("io.github.belgif.rest.guide.validator.OpenApiValidator")
                .build();

        Testcase testcase = Testcase.builder()
                .classname("io.github.belgif.rest.guide.validator.OpenApiValidator")
                .name("testOk")
                .time("1")
                .build();
        testsuite.addTestcase(testcase);

        testcase = Testcase.builder()
                .classname("io.github.belgif.rest.guide.validator.OpenApiValidator")
                .name("testFail")
                .time("2")
                .failure(new Failure("org.opentest4j.AssertionFailedError:", null, "message"))
                .build();
        testsuite.addTestcase(testcase);

        testcase = Testcase.builder()
                .classname("io.github.belgif.rest.guide.validator.OpenApiValidator")
                .name("testError")
                .time("3")
                .error(new Error("java.RuntimeException", null, "message"))
                .build();
        testsuite.addTestcase(testcase);

        try {
            var outputProcessor = new JUnitOutputProcessor(OutputGroupBy.RULE);
            outputProcessor.setOutput(Files.createTempDirectory("tmp").toFile());
            outputProcessor.write(testsuite);
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void process() {
        try {
            var outputProcessor = new JUnitOutputProcessor(OutputGroupBy.RULE);
            outputProcessor.setOutput(Files.createTempDirectory("tmp").toFile());
            outputProcessor.process(getViolationAggregator());
        } catch (IOException e) {
            fail(e);
        }
    }

    private OpenApiViolationAggregator getViolationAggregator() throws IOException {
        var openApiViolationAggregator = new OpenApiViolationAggregator();
        openApiViolationAggregator.setTime(0.55f);
        openApiViolationAggregator.setRuleNumber(5);
        openApiViolationAggregator.addViolation("Rule-test", "The rume message", new Line("", 155), "/myPointer");

        return openApiViolationAggregator;
    }

}