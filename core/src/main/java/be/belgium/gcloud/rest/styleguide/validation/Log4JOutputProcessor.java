package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class Log4JOutputProcessor implements OutputProcessor {
    @Override
    public void process(OpenApiViolationAggregator violationAggregator) {
        Collections.sort(violationAggregator.getViolations());

        log.debug("\n {} OpenApi Violations.", violationAggregator.getViolations().size());

        violationAggregator.getViolations().forEach(v -> {
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
    }
}
