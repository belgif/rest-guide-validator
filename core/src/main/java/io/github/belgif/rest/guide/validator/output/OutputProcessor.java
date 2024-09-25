package io.github.belgif.rest.guide.validator.output;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.Violation;
import lombok.Getter;

import java.util.*;

@Getter
public abstract class OutputProcessor {

    private OutputGroupBy outputGroupBy;
    private final List<ViolationReport> aggregators = new ArrayList<>();

    protected OutputProcessor(OutputGroupBy outputGroupBy) {
        this.setOutputGroupBy(outputGroupBy);
    }

    public void addAggregator(ViolationReport aggregator) {
        aggregators.add(aggregator);
    }

    public void process() {
        process(ViolationReport.aggregate(aggregators));
    }
    protected abstract void process(ViolationReport violationAggregator);

    public void setOutputGroupBy(OutputGroupBy outputGroupBy) {
        this.outputGroupBy = Objects.requireNonNullElse(outputGroupBy, OutputGroupBy.RULE);
    }

    protected String getOccurrences(List<Violation> violations) {
        return violations.size() == 1 ? "1 occurrence:" : violations.size() + " occurrences:";
    }

}
