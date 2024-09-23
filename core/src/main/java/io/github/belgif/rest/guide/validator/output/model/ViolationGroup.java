package io.github.belgif.rest.guide.validator.output.model;

import java.util.List;

public record ViolationGroup(
        String group,
        int occurrences,
        List<ViolationFileObject> violations) {
}
