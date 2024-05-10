package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.core.ViolationType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Log4JOutputProcessor implements OutputProcessor {
    @Override
    public void process(OpenApiViolationAggregator violationAggregator) {
        List<Violation> violations = violationAggregator.getViolations().stream().filter(v -> v.type != ViolationType.IGNORED).sorted().collect(Collectors.toList());
        List<Violation> ignored = violationAggregator.getViolations().stream().filter(v -> v.type == ViolationType.IGNORED).sorted().collect(Collectors.toList());

        log.debug("\n OpenApi validation summary: {} violations and {} ignored violations.", violations.size(), ignored.size());
        violations.forEach(v -> {
            switch (v.type) {
                case MANDATORY:
                    log.error(v.toString());
                    break;
                case RECOMMENDED:
                    log.warn(v.toString());
                    break;
                case STYLE:
                    log.debug(v.toString());
                    break;
                default:
                    log.info(v.toString());
            }
        });
        ignored.forEach(v -> log.info(v.toString()));
    }
}
