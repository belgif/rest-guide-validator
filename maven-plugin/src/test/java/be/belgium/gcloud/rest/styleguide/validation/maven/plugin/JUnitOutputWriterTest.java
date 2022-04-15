package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import be.belgium.gcloud.rest.styleguide.validation.JUnitOutputProcessor;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Error;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Failure;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Testcase;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Testsuite;
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
                .name("be.belgium.gcloud.rest.styleguide.validation.OpenApiValidator")
                .build();

        Testcase testcase = Testcase.builder()
                .classname("be.belgium.gcloud.rest.styleguide.validation.OpenApiValidator")
                .name("testOk")
                .time("1")
                .build();
        testsuite.addTestcase(testcase);

        testcase = Testcase.builder()
                .classname("be.belgium.gcloud.rest.styleguide.validation.OpenApiValidator")
                .name("testFail")
                .time("2")
                .failure(new Failure("org.opentest4j.AssertionFailedError:", null, "message"))
                .build();
        testsuite.addTestcase(testcase);

        testcase = Testcase.builder()
                .classname("be.belgium.gcloud.rest.styleguide.validation.OpenApiValidator")
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
        openApiViolationAggregator.addViolation("Rule-test", "The rume message", 155);

        return openApiViolationAggregator;
    }

}