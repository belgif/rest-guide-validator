package io.github.belgif.rest.guide.validator.runner.output;

import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.core.ViolationReport;

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
            System.err.println(groupLine);
            System.err.flush();
            violationList.forEach(v -> {
                System.err.println(v.getReportMessage());
                System.err.flush();
            });
        });

        if (!violationReport.isOasValid()) {
            System.err.println(VIOLATION_INFO_MESSAGE);
            System.err.flush();
        }
    }
}
