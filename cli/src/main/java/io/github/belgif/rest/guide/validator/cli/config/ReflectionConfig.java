package io.github.belgif.rest.guide.validator.cli.config;

import io.github.belgif.rest.guide.validator.runner.output.junit.Error;
import io.github.belgif.rest.guide.validator.runner.output.junit.Failure;
import io.github.belgif.rest.guide.validator.runner.output.junit.Testcase;
import io.github.belgif.rest.guide.validator.runner.output.junit.Testsuite;
import io.github.belgif.rest.guide.validator.runner.output.model.OutputViolationReport;
import io.github.belgif.rest.guide.validator.runner.output.model.ViolationEntry;
import io.github.belgif.rest.guide.validator.runner.output.model.ViolationGroup;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Reflection config for Jackson/JAXB serialization - https://quarkus.io/guides/writing-native-applications-tips#registering-for-reflection.
 * We could also add this annotation to each of the specific classes
 */
@RegisterForReflection(targets ={
        OutputViolationReport.class, ViolationGroup.class, ViolationEntry.class, // for JSON output
        Testsuite.class, Testcase.class, Failure.class, Error.class // for JUNIT output - JAXB - properties with classes not included will be ignored silently
})
public class ReflectionConfig {
}
