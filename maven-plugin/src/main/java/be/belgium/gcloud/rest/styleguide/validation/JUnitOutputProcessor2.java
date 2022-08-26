package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.Violation;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Failure;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Testcase;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Testsuite;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Output processor to write a junit xml test result.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class JUnitOutputProcessor2 implements OutputProcessor {
    private static final String RULE_PACKAGE = "be.belgium.gcloud.rest.styleguide.validation.core.rules";
    private File outputDir;

    private static  String getNameUpFirst(String name){
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    /**
     * Write the testsuite to an XML file.
     * @param testsuite
     */
    public void write(Testsuite testsuite){
        if (testsuite == null)
            throw new IllegalArgumentException("testsuite cannot be null");
        if(outputDir == null || ! outputDir.isDirectory() || ! outputDir.canWrite())
            throw new IllegalArgumentException(outputDir + " must be a writable directory");
        try {
            var mar= JAXBContext.newInstance(Testsuite.class).createMarshaller();
            mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            mar.marshal(testsuite, new File(outputDir, "TEST-" + testsuite.getName() + ".xml"));
        } catch (JAXBException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Write a XML JUnit file using the violationAggregator.
     * @param violationAggregator
     */
    @Override
    public void process(OpenApiViolationAggregator violationAggregator) {
        Map<String, List<Violation>> violations = violationAggregator.getViolations().stream()
                .collect(Collectors.groupingBy(Violation::getRuleName));

        violations.keySet().forEach(k->{
            var violationList = violations.get(k);
            var testsuite = Testsuite.builder()
                    .name(getNameUpFirst(k))
                    .timestamp(LocalDateTime.now().toString())
                    .tests(violationList.size())
                    .build();

            violationList.forEach(v->{
                Testcase testcase = Testcase.builder()
                        .classname(getNameUpFirst(k))
                        .name(" -> line: " + String.valueOf(v.getLineNumber()))
                        .failure(new Failure(v.getType().name(), v.getMessage(), ""))
                        .build();
                testsuite.addTestcase(testcase);
            });

            write(testsuite);
        });

    }
}
