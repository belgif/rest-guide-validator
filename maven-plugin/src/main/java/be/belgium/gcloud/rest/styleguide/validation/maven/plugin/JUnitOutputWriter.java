package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import be.belgium.gcloud.rest.styleguide.validation.OutputProcessor;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Failure;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Testcase;
import be.belgium.gcloud.rest.styleguide.validation.maven.junit.Testsuite;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
@Slf4j
public class JUnitOutputWriter implements OutputProcessor {
    private final File outputFile;

    public void write(Testsuite testsuite){
        if (testsuite == null)
            throw new IllegalArgumentException("testsuites cannot be null");
        if(outputFile == null || ! outputFile.canWrite())
            throw new IllegalArgumentException("outputFile cannot be null and must be writable");
        try {
            Marshaller mar= JAXBContext.newInstance(Testsuite.class).createMarshaller();
            mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            mar.marshal(testsuite, outputFile);
            //mar.marshal(testsuite, System.out);
        } catch (JAXBException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void process(OpenApiViolationAggregator violationAggregator) {
        Testsuite testsuite = Testsuite.builder()
                .name("be.belgium.gcloud.rest.styleguide.validation.maven.plugin.OpenApi")
                .pkg("be.belgium.gcloud.rest.styleguide.validation.maven.plugin")
                .timestamp(LocalDateTime.now().toString())
                .tests(violationAggregator.getRuleNumber())
                .time(violationAggregator.getTime())
                .build();

        violationAggregator.getViolations().forEach(violation -> {
            Testcase testcase = Testcase.builder()
                    .classname("be.belgium.gcloud.rest.styleguide.validation.core.rules")
                    .name(violation.getRuleName())
                    .failure(new Failure(violation.getType().name(), "Line: " + String.valueOf(violation.getLineNumber()), violation.getMessage()))
                    .build();
            testsuite.addTestcase(testcase);
        });

        write(testsuite);
    }
}
