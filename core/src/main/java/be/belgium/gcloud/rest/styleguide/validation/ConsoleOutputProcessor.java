package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;

import java.util.Collections;

public class ConsoleOutputProcessor implements OutputProcessor{
    @Override
    public void process(OpenApiViolationAggregator violationAggregator) {
        Collections.sort(violationAggregator.getViolations());
        System.out.printf("\n%d OpenApi Violations for: %s \n", violationAggregator.getViolations().size(), violationAggregator.getOpenApiFile().getAbsolutePath()); //NOSONAR

        violationAggregator.getViolations().forEach(v->{
            switch (v.type){
                case MANDATORY:
                    System.err.println(v.toString()); break; //NOSONAR
                case RECOMMENDED:
                    System.out.println(v.toString()); break; //NOSONAR
                case STYLE:
                    System.out.println(v.toString()); break; //NOSONAR
                default:
                    System.out.println(v.toString()); //NOSONAR
            }
        });
    }
}
