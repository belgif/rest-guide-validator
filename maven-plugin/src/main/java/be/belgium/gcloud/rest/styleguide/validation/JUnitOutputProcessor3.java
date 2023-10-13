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
import java.util.stream.Collectors;

/**
 * Output processor to write a junit xml test result.
 * This processor aggregate all tests (ignore the source files) and group it by rules.
 * DO NOT use it if the files are about different apis.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class JUnitOutputProcessor3 implements OutputProcessor, DirectoryOutputProcessor {
    private static final String RULE_PACKAGE = "be.belgium.gcloud.rest.styleguide.validation.core.rules";

    /**
     * Output directory.
     */
    private File output;

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
        if(output == null || ! output.isDirectory() || ! output.canWrite())
            throw new IllegalArgumentException(output + " must be a writable directory");
        try {
            var mar= JAXBContext.newInstance(Testsuite.class).createMarshaller();
            mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            mar.marshal(testsuite, new File(output, "TEST-" + testsuite.getName() + ".xml"));
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
                    .failures(violationList.size())
                    .build();
            var sample = violations.get(k).get(0);
            var testcase = Testcase.builder()
                    .classname(getNameUpFirst(k))
                    .name(sample.getType().name())
                    .failure(new Failure(sample.getType().name(), sample.getMessage().split("\t")[0], ""))
                    .build();
            testsuite.addTestcase(testcase);

            violationList.forEach(v->{
                testcase.appendSysOut(v.getLineNumber().getFileName(), v.getLineNumber().getLineNumber(), getMessageDetail(v));
            });

            write(testsuite);
        });
    }

    private String getMessageDetail(Violation v){
        var messages = v.getMessage().split("\t");
        return messages.length > 1  ? messages[1] : null ;
    }
}