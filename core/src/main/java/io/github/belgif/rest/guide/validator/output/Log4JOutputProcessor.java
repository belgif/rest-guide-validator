package io.github.belgif.rest.guide.validator.output;

import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
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
    public void process(OpenApiViolationAggregator violationAggregator) {
        List<Violation> violations = violationAggregator.getViolations();

        System.out.printf("%n OpenApi validation summary: %d violations and %d ignored violations.%n", violationAggregator.getAmountOfActionableViolations(), violationAggregator.getAmountOfIgnoredViolations());

        Map<String, List<Violation>> groupedViolations = this.getOutputGroupBy().groupViolations(violations);

        groupedViolations.forEach((group, violationList) -> {
            var groupViolation = violationList.get(0);
            var groupLine = group + " " + getOccurrences(violationList);
            switch (groupViolation.getType()) {
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
                switch (v.getType()) {
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