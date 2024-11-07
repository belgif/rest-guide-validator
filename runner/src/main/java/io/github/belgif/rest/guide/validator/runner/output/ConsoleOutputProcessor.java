package io.github.belgif.rest.guide.validator.runner.output;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.core.ViolationLevel;

import java.util.List;
import java.util.Map;

public class ConsoleOutputProcessor extends OutputProcessor {

    public ConsoleOutputProcessor(OutputGroupBy outputGroupBy) {
        super(outputGroupBy);
    }

    @Override
    public void process(ViolationReport violationReport) {
        List<Violation> violations = violationReport.getViolations();

        System.out.printf("%n OpenApi validation summary: %d violations and %d ignored violations.%n", violationReport.getAmountOfActionableViolations(), violationReport.getAmountOfIgnoredViolations());

        Map<String, List<Violation>> groupedViolations = this.getOutputGroupBy().groupViolations(violations);

        groupedViolations.forEach((group, violationList) -> {
            var groupLine = group + " " + getOccurrences(violationList);
            if (violationList.get(0).getLevel() == ViolationLevel.MANDATORY) {
                System.err.println(groupLine);
                System.err.flush();
            } else {
                System.out.println(groupLine);
                System.out.flush();
            }
            violationList.forEach(v -> {
                if (v.getLevel() == ViolationLevel.MANDATORY) {
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
