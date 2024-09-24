package io.github.belgif.rest.guide.validator.output.model;

import java.util.List;

public record ViolationGroup(
        int total,
        List<ViolationEntry> occurrences) {
}
