package io.github.belgif.rest.guide.validator.output.model;

import java.util.List;

public record ViolationReport(
        int totalViolations,
        int totalIgnoredViolations,
        String groupedBy,
        List<ViolationGroup> violations
) {
}
