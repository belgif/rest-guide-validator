package io.github.belgif.rest.guide.validator.output.model;

public record ViolationFileObject(
        String ruleId,
        String description,
        String message,
        String type,
        String fileName,
        int lineNumber,
        String pointer) {
}
