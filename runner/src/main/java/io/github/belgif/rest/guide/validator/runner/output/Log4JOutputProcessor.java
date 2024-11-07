package io.github.belgif.rest.guide.validator.runner.output;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.Violation;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class Log4JOutputProcessor extends OutputProcessor {

    public Log4JOutputProcessor(OutputGroupBy outputGroupBy) {
        super(outputGroupBy);
    }

    @Override
    public void process(ViolationReport violationReport) {
        List<Violation> violations = violationReport.getViolations();

        System.out.printf("%n OpenApi validation summary: %d violations and %d ignored violations.%n", violationReport.getAmountOfActionableViolations(), violationReport.getAmountOfIgnoredViolations());

        Map<String, List<Violation>> groupedViolations = this.getOutputGroupBy().groupViolations(violations);

        groupedViolations.forEach((group, violationList) -> {
            var groupViolation = violationList.get(0);
            var groupLine = group + " " + getOccurrences(violationList);
            switch (groupViolation.getLevel()) {
                case MANDATORY:
                    log.error(groupLine);
                    break;
                case RECOMMENDED:
                    log.warn(groupLine);
                    break;
                case STYLE:
                    log.debug(groupLine);
                    break;
                default:
                    log.info(groupLine);
            }
            violationList.forEach(v -> {
                switch (v.getLevel()) {
                    case MANDATORY:
                        log.error(v.getReportMessage());
                        break;
                    case RECOMMENDED:
                        log.warn(v.getReportMessage());
                        break;
                    case STYLE:
                        log.debug(v.getReportMessage());
                        break;
                    default:
                        log.info(v.getReportMessage());
                }
            });
        });

    }
}
