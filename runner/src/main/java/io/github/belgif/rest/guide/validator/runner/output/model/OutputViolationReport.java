package io.github.belgif.rest.guide.validator.runner.output.model;

import java.util.Map;

public record OutputViolationReport(
        int totalViolations,
        int totalIgnoredViolations,
        String groupedBy,
        Map<String, ViolationGroup> violations
) {
}
