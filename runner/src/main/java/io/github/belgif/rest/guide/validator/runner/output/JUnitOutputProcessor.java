package io.github.belgif.rest.guide.validator.runner.output;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.core.ViolationLevel;
import io.github.belgif.rest.guide.validator.runner.output.junit.Failure;
import io.github.belgif.rest.guide.validator.runner.output.junit.Testcase;
import io.github.belgif.rest.guide.validator.runner.output.junit.Testsuite;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Output processor to write a junit xml test result.
 * This processor aggregate all tests (ignore the source files) and group it by rules.
 * DO NOT use it if the files are about different apis.
 */
@Getter
@Setter
@Slf4j
public class JUnitOutputProcessor extends OutputProcessor {
    /**
     * Output directory.
     */
    private final File outputDirectory;

    private final long start;

    public JUnitOutputProcessor(OutputGroupBy outputGroupBy, File outputDirectory) {
        super(outputGroupBy);
        this.outputDirectory = outputDirectory;
        this.start = System.currentTimeMillis();
    }


    public void write(Testsuite testsuite) {
        if (testsuite == null)
            throw new IllegalArgumentException("testsuite cannot be null");
        if (outputDirectory == null || !outputDirectory.isDirectory() || !outputDirectory.canWrite())
            throw new IllegalArgumentException(outputDirectory + " must be a writable directory");
        try {
            var mar = JAXBContext.newInstance(Testsuite.class).createMarshaller();
            mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            mar.marshal(testsuite, new File(outputDirectory, "TEST-" + testsuite.getName() + ".xml"));
        } catch (JAXBException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void process(ViolationReport violationReport) {
        Map<String, List<Violation>> groupedViolations = this.getOutputGroupBy().groupViolations(violationReport.getViolations());
        var testSuites = findTestSuites(groupedViolations);
        testSuites.forEach((identifier, violationsKeys) -> {
            var allViolations = violationsKeys.stream().map(groupedViolations::get).flatMap(Collection::stream).toList();
            var amountOfTests = allViolations.size();
            var failures = (int) allViolations.stream().filter(v -> !v.getLevel().equals(ViolationLevel.IGNORED)).count();
            var skipped = amountOfTests - failures;

            var testsuite = Testsuite.builder()
                    .name(identifier)
                    .timestamp(LocalDateTime.now().toString())
                    .tests(amountOfTests)
                    .failures(failures)
                    .skipped(skipped)
                    .build();

            violationsKeys.forEach(key -> {
                var violationsForGroup = groupedViolations.get(key);
                var sample = violationsForGroup.get(0);
                var testcaseBuilder = Testcase.builder()
                        .classname(key)
                        .name(sample.getLevel().name());
                if (sample.getLevel() != ViolationLevel.IGNORED) {
                    testcaseBuilder.failure(new Failure(sample.getLevel().name(), key, ""));
                } else {
                    testcaseBuilder.skipped(key);
                }

                var testcase = testcaseBuilder.time(calculateTime()+"").build();
                violationsForGroup.forEach(v -> testcase.appendSysOut(v.getReportMessage()));
                testsuite.addTestcase(testcase);
            });
            write(testsuite);
        });
    }

    private Map<String, List<String>> findTestSuites(Map<String, List<Violation>> groupedViolations) {
        Map<String, List<String>> testSuitesGroupedViolationMap = new LinkedHashMap<>();
        groupedViolations.keySet().forEach(group -> {
            var identifier = this.getOutputGroupBy().getIdentifier(groupedViolations.get(group).get(0));
            if (testSuitesGroupedViolationMap.containsKey(identifier)) {
                testSuitesGroupedViolationMap.get(identifier).add(group);
            } else {
                List<String> groupedViolationsKeys = new ArrayList<>();
                groupedViolationsKeys.add(group);
                testSuitesGroupedViolationMap.put(identifier, groupedViolationsKeys);
            }
        });
        return testSuitesGroupedViolationMap;
    }

    private float calculateTime() {
        return (System.currentTimeMillis() - start) / 1000f;
    }
}