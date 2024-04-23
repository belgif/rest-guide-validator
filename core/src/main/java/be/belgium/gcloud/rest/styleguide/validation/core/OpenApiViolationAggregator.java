package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.core.model.OpenApiDefinition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

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

    public void addViolation(String ruleName, String message, Line lineNumber, ViolationType type, String jsonPointer) {
        this.violations.add(Violation.builder()
                .ruleName(ruleName)
                .message(message)
                .lineNumber(lineNumber)
                .type(type)
                .pointer(jsonPointer).build());
    }

    public void addViolation(String ruleName, String message, Line lineNumber, String jsonPointer) {
        this.addViolation(ruleName, message, lineNumber, ViolationType.MANDATORY, jsonPointer);
    }

    public void addViolation(String ruleName, String message, OpenApiDefinition<?> openApiDefinition, ViolationType violationType) {
        if (addExcludedFileViolation(openApiDefinition)) {
            return;
        }
        if (addIgnoredViolation(ruleName, openApiDefinition)) {
            return;
        }
        Line lineNumber = openApiDefinition.getLineNumber();
        this.addViolation(ruleName, message, lineNumber, violationType, openApiDefinition.getJsonPointer().toPrettyString());
    }

    public void addViolation(String ruleName, String message, OpenApiDefinition<?> openApiDefinition) {
        this.addViolation(ruleName, message, openApiDefinition, ViolationType.MANDATORY);
    }

    public void addViolation(String ruleName, String message) {
        this.addViolation(ruleName, message, new Line("", 0), ViolationType.MANDATORY, "");
    }

    private boolean addExcludedFileViolation(OpenApiDefinition<?> openApiDefinition) {
        Path relativePath = openApiDefinition.getResult().getOpenApiFile().getParentFile().toPath().relativize(openApiDefinition.getOpenApiFile().toPath());
        if (fileShouldBeExcluded(relativePath)) {
            var fileName = openApiDefinition.getOpenApiFile().getName();
            if(!isExcludedFileInViolations(fileName)) {
                this.addViolation("[ignored-file]", "File ignored: " + fileName, new Line(fileName, 0), ViolationType.IGNORED, openApiDefinition.getJsonPointer().toPrettyString());
            }
            return true;
        }
        return false;
    }

    private boolean addIgnoredViolation(String ruleName, OpenApiDefinition<?> openApiDefinition) {
        if (!openApiDefinition.getIgnoredRules().isEmpty()) {
            String lookupRuleName = ruleName.replace("[", "").replace("]", "");
            if (openApiDefinition.getIgnoredRules().containsKey(lookupRuleName)) {
                Line lineNumber = openApiDefinition.getLineNumber();
                var violationType = ViolationType.IGNORED;
                var message = "Ignored: " + openApiDefinition.getIgnoredRules().get(lookupRuleName);
                this.addViolation(ruleName, message, lineNumber, violationType, openApiDefinition.getJsonPointer().toPrettyString());
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
