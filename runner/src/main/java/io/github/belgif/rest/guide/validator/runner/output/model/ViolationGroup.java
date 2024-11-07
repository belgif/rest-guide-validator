package io.github.belgif.rest.guide.validator.runner.output.model;

import java.util.List;

public record ViolationGroup(
        int total,
        List<ViolationEntry> occurrences) {
}
