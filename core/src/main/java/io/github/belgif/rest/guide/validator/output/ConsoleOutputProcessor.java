package io.github.belgif.rest.guide.validator.output;

import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.core.ViolationType;

import java.util.List;
import java.util.Map;

public class ConsoleOutputProcessor extends OutputProcessor {

    public ConsoleOutputProcessor(OutputGroupBy outputGroupBy) {
        super(outputGroupBy);
    }

    @Override
    public void process(OpenApiViolationAggregator violationAggregator) {
        List<Violation> violations = violationAggregator.getViolations();

        System.out.printf("%n OpenApi validation summary: %d violations and %d ignored violations.%n", violationAggregator.getAmountOfActionableViolations(), violationAggregator.getAmountOfIgnoredViolations());

        Map<String, List<Violation>> groupedViolations = this.getOutputGroupBy().groupViolations(violations);

        groupedViolations.forEach((group, violationList) -> {
            var groupLine = group + " " + getOccurrences(violationList);
            if (violationList.get(0).getType() == ViolationType.MANDATORY) {
                System.err.println(groupLine);
                System.err.flush();
            } else {
                System.out.println(groupLine);
                System.out.flush();
            }
            violationList.forEach(v -> {
                if (v.getType() == ViolationType.MANDATORY) {
                    System.err.println(v.getReportMessage());
                    System.err.flush();
                } else {
                    System.out.println(v.getReportMessage());
                    System.out.flush();
                }
            });
        });
    }
}
