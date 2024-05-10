package io.github.belgif.rest.styleguide.validation;

import io.github.belgif.rest.styleguide.validation.core.OpenApiViolationAggregator;
import io.github.belgif.rest.styleguide.validation.maven.junit.Failure;
import io.github.belgif.rest.styleguide.validation.maven.junit.Testcase;
import io.github.belgif.rest.styleguide.validation.maven.junit.Testsuite;
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

/**
 * Output processor to write a junit xml test result.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class JUnitOutputProcessor implements OutputProcessor {
    private File outputFile;

    /**
     * Write the testsuite to an XML file.
     * @param testsuite
     */
    public void write(Testsuite testsuite){
        if (testsuite == null)
            throw new IllegalArgumentException("testsuite cannot be null");
        if(outputFile == null )
            throw new IllegalArgumentException("outputFile cannot be null and must be writable");
        try {
            Marshaller mar= JAXBContext.newInstance(Testsuite.class).createMarshaller();
            mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            mar.marshal(testsuite, outputFile);
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
        Testsuite testsuite = Testsuite.builder()
                .name("io.github.belgif.rest.styleguide.validation.core.rules")
              //  .pkg(this.getClass().getPackageName())
                .timestamp(LocalDateTime.now().toString())
                .tests(violationAggregator.getRuleNumber())
                .time(violationAggregator.getTime())
                .build();

        violationAggregator.getViolations().forEach(violation -> {
            Testcase testcase = Testcase.builder()
                    .classname("io.github.belgif.rest.styleguide.validation.core.rules"+violation.getRuleName())
                    .name(violation.getRuleName())
                    .failure(new Failure(violation.getType().name(), "Line: " + String.valueOf(violation.getLineNumber()), violation.getMessage()))
                    .build();
            testsuite.addTestcase(testcase);
        });

        write(testsuite);
    }
}
