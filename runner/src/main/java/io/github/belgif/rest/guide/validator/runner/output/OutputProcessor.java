package io.github.belgif.rest.guide.validator.runner.output;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.Violation;
import lombok.Getter;

import java.util.*;

@Getter
public abstract class OutputProcessor {

    protected static final String VIOLATION_INFO_MESSAGE = """
            
            https://www.rfc-editor.org/rfc/rfc2119#section-1
            "REQUIRED" means that the definition is an absolute requirement of the Belgif REST guide specification.
            
            "RECOMMENDED" means that there may exist valid reasons in particular circumstances to ignore a particular rule, but the full implications must be understood and carefully weighed before choosing a different course.
            
            To ignore a rule, use `x-ignore-rules` and provide a motivation, as described in https://github.com/belgif/rest-guide-validator/blob/main/readme.md#exclusions-in-an-openapi-file""";

    private OutputGroupBy outputGroupBy;

    protected OutputProcessor(OutputGroupBy outputGroupBy) {
        this.setOutputGroupBy(outputGroupBy);
    }

    public abstract void process(ViolationReport violationReport);

    public void setOutputGroupBy(OutputGroupBy outputGroupBy) {
        this.outputGroupBy = Objects.requireNonNullElse(outputGroupBy, OutputGroupBy.RULE);
    }

    protected String getOccurrences(List<Violation> violations) {
        return violations.size() == 1 ? "1 occurrence:" : violations.size() + " occurrences:";
    }

}
