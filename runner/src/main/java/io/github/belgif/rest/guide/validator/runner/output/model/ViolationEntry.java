package io.github.belgif.rest.guide.validator.runner.output.model;

public record ViolationEntry(
        String ruleId,
        String description,
        String message,
        String level,
        String fileName,
        int lineNumber,
        String pointer) {
}
