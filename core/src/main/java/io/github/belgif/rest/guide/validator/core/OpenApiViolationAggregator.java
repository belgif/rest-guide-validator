package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class OpenApiViolationAggregator {
    /**
     * Structure that hold the violations.
     */
    private final List<Violation> violations = new ArrayList<>();
    private List<String> excludedFiles = new ArrayList<>();

    private int ruleNumber;
    private float time;

    public void addViolation(Violation violation) {
        this.violations.add(violation);
    }

    public void addViolation(String ruleName, String description, String message, Line lineNumber, ViolationType type, String jsonPointer) {
        this.addViolation(new Violation(ruleName, description, message, type, lineNumber, jsonPointer));
    }

    public void addViolation(String ruleName, String description, String message, Line lineNumber, String jsonPointer) {
        this.addViolation(ruleName, description, message, lineNumber, ViolationType.MANDATORY, jsonPointer);
    }

    public void addViolation(String ruleName, String description, Line lineNumber, String jsonPointer) {
        this.addViolation(ruleName, description, null, lineNumber, ViolationType.MANDATORY, jsonPointer);
    }

    public void addViolation(String ruleName, String description, String message, OpenApiDefinition<?> openApiDefinition, ViolationType violationType) {
        if (addExcludedFileViolation(openApiDefinition) || addIgnoredViolation(ruleName, description, openApiDefinition)) {
            return;
        }
        Line lineNumber = openApiDefinition.getLineNumber();
        this.addViolation(ruleName, description, message, lineNumber, violationType, openApiDefinition.getPrintableJsonPointer());
    }

    public void addViolation(String ruleName, String description, String message, OpenApiDefinition<?> openApiDefinition) {
        this.addViolation(ruleName, description, message, openApiDefinition, ViolationType.MANDATORY);
    }

    public void addViolation(String ruleName, String description, OpenApiDefinition<?> openApiDefinition) {
        this.addViolation(ruleName, description, null, openApiDefinition, ViolationType.MANDATORY);
    }

    public void addViolation(String ruleName, String description, String message) {
        this.addViolation(ruleName, description, message, new Line("", 0), ViolationType.MANDATORY, "");
    }

    public void addViolation(String ruleName, String description) {
        this.addViolation(ruleName, description, null, new Line("", 0), ViolationType.MANDATORY, "");
    }

    public List<Violation> getActionableViolations() {
        return this.violations.stream().filter(v -> v.getType() != ViolationType.IGNORED).sorted().collect(Collectors.toList());
    }

    public List<Violation> getIgnoredViolations() {
        return this.violations.stream().filter(v -> v.getType() == ViolationType.IGNORED).sorted().collect(Collectors.toList());
    }

    public int getAmountOfActionableViolations() {
        return getActionableViolations().size();
    }

    public int getAmountOfIgnoredViolations() {
        return getIgnoredViolations().size();
    }

    private boolean addExcludedFileViolation(OpenApiDefinition<?> openApiDefinition) {
        Path relativePath = openApiDefinition.getResult().getOpenApiFile().getParentFile().toPath().relativize(openApiDefinition.getOpenApiFile().toPath());
        if (fileShouldBeExcluded(relativePath)) {
            var fileName = openApiDefinition.getOpenApiFile().getName();
            if (!isExcludedFileInViolations(fileName)) {
                this.addViolation("[ignored-file]", "File ignored: " + fileName, null, new Line(fileName, 0), ViolationType.IGNORED, "");
            }
            return true;
        }
        return false;
    }

    private boolean addIgnoredViolation(String ruleName, String description, OpenApiDefinition<?> openApiDefinition) {
        if (!openApiDefinition.getIgnoredRules().isEmpty()) {
            String lookupRuleName = ruleName.replace("[", "").replace("]", "");
            if (openApiDefinition.getIgnoredRules().containsKey(lookupRuleName)) {
                Line lineNumber = openApiDefinition.getLineNumber();
                var violationType = ViolationType.IGNORED;
                var message = openApiDefinition.getIgnoredRules().get(lookupRuleName);
                this.addViolation(ruleName, description, message, lineNumber, violationType, openApiDefinition.getPrintableJsonPointer());
                return true;
            }
        }
        return false;
    }

    private boolean isExcludedFileInViolations(String fileName) {
        return violations.stream().anyMatch(violation -> violation.getLineNumber().getFileName().equals(fileName));
    }

    private boolean fileShouldBeExcluded(Path relativePath) {
        if (excludedFiles != null) {
            for (String pattern : excludedFiles) {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                if (matcher.matches(relativePath)) {
                    return true;
                }
            }
        }
        return false;
    }

}