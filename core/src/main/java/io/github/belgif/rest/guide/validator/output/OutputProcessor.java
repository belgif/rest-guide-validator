package io.github.belgif.rest.guide.validator.output;

import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.core.Violation;
import lombok.Getter;

import java.util.*;

@Getter
public abstract class OutputProcessor {

    private OutputGroupBy outputGroupBy;
    private final List<OpenApiViolationAggregator> aggregators = new ArrayList<>();

    protected OutputProcessor(OutputGroupBy outputGroupBy) {
        this.setOutputGroupBy(outputGroupBy);
    }

    public void addAggregator(OpenApiViolationAggregator aggregator) {
        aggregators.add(aggregator);
    }

    public void process() {
        process(OpenApiViolationAggregator.aggregate(aggregators));
    }
    protected abstract void process(OpenApiViolationAggregator violationAggregator);

    public void setOutputGroupBy(OutputGroupBy outputGroupBy) {
        this.outputGroupBy = Objects.requireNonNullElse(outputGroupBy, OutputGroupBy.RULE);
    }

    protected String getOccurrences(List<Violation> violations) {
        return violations.size() == 1 ? "1 occurrence:" : violations.size() + " occurrences:";
    }

}
