package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;

import java.util.Collections;

public class ConsoleOutputProcessor implements OutputProcessor{
    @Override
    public void process(OpenApiViolationAggregator violationAggregator) {
        Collections.sort(violationAggregator.getViolations());
        if( ! violationAggregator.getViolations().isEmpty()){
            System.out.printf("\n %d OpenApi Violations for: %s ", violationAggregator.getViolations().size(), violationAggregator.getOpenApiFile().getAbsolutePath());
        }
        violationAggregator.getViolations().forEach(v->{
            switch (v.type){
                case MANDATORY:
                    System.err.println(v.toString()); break;
                case RECOMMENDED:
                    System.out.println(v.toString()); break;
                case STYLE:
                    System.out.println(v.toString()); break;
                default:
                    System.out.println(v.toString());
            }
        });
    }
}
