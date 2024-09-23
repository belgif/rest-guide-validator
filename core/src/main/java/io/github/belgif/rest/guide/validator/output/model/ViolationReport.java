package io.github.belgif.rest.guide.validator.output.model;

import java.util.LinkedHashMap;

public record ViolationReport(
        int totalViolations,
        int totalIgnoredViolations,
        String groupedBy,
        LinkedHashMap<String, ViolationGroup> violations
) {
}
