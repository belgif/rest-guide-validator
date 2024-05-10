package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;

public interface OutputProcessor {
    void process(OpenApiViolationAggregator violationAggregator);
}
