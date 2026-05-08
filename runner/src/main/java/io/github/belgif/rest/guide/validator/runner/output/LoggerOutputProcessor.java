package io.github.belgif.rest.guide.validator.runner.output;

import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class LoggerOutputProcessor extends OutputProcessor {

    public LoggerOutputProcessor(OutputGroupBy outputGroupBy) {
        super(outputGroupBy);
    }

    @Override
    public void process(ViolationReport violationReport) {
        List<Violation> violations = violationReport.getViolations();

        System.out.printf("%n OpenApi validation summary: %d violations and %d ignored violations.%n", violationReport.getAmountOfActionableViolations(), violationReport.getAmountOfIgnoredViolations());

        Map<String, List<Violation>> groupedViolations = this.getOutputGroupBy().groupViolations(violations);

        groupedViolations.forEach((group, violationList) -> {
            var groupLine = group + " " + getOccurrences(violationList);
            log.error(groupLine);
            violationList.forEach(v ->
                    log.error(v.getReportMessage()));
        });

        if (!violationReport.isOasValid()) {
            log.error(VIOLATION_INFO_MESSAGE);
        }
    }
}
