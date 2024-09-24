package io.github.belgif.rest.guide.validator.output.model;

import java.util.Map;

public record ViolationReport(
        int totalViolations,
        int totalIgnoredViolations,
        String groupedBy,
        Map<String, ViolationGroup> violations
) {
}
