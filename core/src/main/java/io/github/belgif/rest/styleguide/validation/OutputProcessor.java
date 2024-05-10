package io.github.belgif.rest.styleguide.validation;

import io.github.belgif.rest.styleguide.validation.core.OpenApiViolationAggregator;

public interface OutputProcessor {
    void process(OpenApiViolationAggregator violationAggregator);
}
