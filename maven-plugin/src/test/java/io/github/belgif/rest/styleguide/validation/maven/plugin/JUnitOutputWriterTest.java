package io.github.belgif.rest.styleguide.validation.maven.plugin;

import io.github.belgif.rest.styleguide.validation.JUnitOutputProcessor;
import io.github.belgif.rest.styleguide.validation.core.Line;
import io.github.belgif.rest.styleguide.validation.core.OpenApiViolationAggregator;
import io.github.belgif.rest.styleguide.validation.maven.junit.Error;
import io.github.belgif.rest.styleguide.validation.maven.junit.Failure;
import io.github.belgif.rest.styleguide.validation.maven.junit.Testcase;
import io.github.belgif.rest.styleguide.validation.maven.junit.Testsuite;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

class JUnitOutputWriterTest {
    @Test
    void writeEmpty() {
        try {
            new JUnitOutputProcessor(File.createTempFile("tmp", "")).write(new Testsuite());
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void write() {
        Testsuite testsuite = Testsuite.builder()
                .name("io.github.belgif.rest.styleguide.validation.OpenApiValidator")
                .build();

        Testcase testcase = Testcase.builder()
                .classname("io.github.belgif.rest.styleguide.validation.OpenApiValidator")
                .name("testOk")
                .time("1")
                .build();
        testsuite.addTestcase(testcase);

        testcase = Testcase.builder()
                .classname("io.github.belgif.rest.styleguide.validation.OpenApiValidator")
                .name("testFail")
                .time("2")
                .failure(new Failure("org.opentest4j.AssertionFailedError:", null, "message"))
                .build();
        testsuite.addTestcase(testcase);

        testcase = Testcase.builder()
                .classname("io.github.belgif.rest.styleguide.validation.OpenApiValidator")
                .name("testError")
                .time("3")
                .error(new Error("java.RuntimeException", null, "message"))
                .build();
        testsuite.addTestcase(testcase);

        try {
            new JUnitOutputProcessor(File.createTempFile("tmp", "")).write(testsuite);
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void process() {
        try {
            new JUnitOutputProcessor(File.createTempFile("tmp", "")).process(getViolationAggregator());
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