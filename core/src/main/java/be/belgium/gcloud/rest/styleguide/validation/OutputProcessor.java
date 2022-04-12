package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;

public interface OutputProcessor {
    void process(OpenApiViolationAggregator violationAggregator);
}
