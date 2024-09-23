package io.github.belgif.rest.guide.validator.output.model;

import java.util.List;

public record ViolationReport(
        int violationCount,
        int ignoredViolationCount,
        String groupedBy,
        List<ViolationGroup> violations
) {
}
