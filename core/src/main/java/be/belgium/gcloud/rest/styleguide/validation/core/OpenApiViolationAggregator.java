package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.core.model.OpenApiDefinition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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

    private int ruleNumber;
    private float time;

    public void addViolation(Violation violation) {
        this.violations.add(violation);
    }

    public void addViolation(String ruleName, String message, Line lineNumber, ViolationType type) {
        this.violations.add(Violation.builder()
                .ruleName(ruleName)
                .message(message)
                .lineNumber(lineNumber)
                .type(type).build());
    }

    public void addViolation(String ruleName, String message, Line lineNumber) {
        this.addViolation(ruleName, message, lineNumber, ViolationType.MANDATORY);
    }

    public void addViolation(String ruleName, String message, OpenApiDefinition<?> openApiDefinition, ViolationType violationType) {
        Line lineNumber = openApiDefinition.getLineNumber();
        this.addViolation(ruleName, message + "\t" + openApiDefinition.getJsonPointer().toPrettyString(), lineNumber, violationType);
    }

    public void addViolation(String ruleName, String message, OpenApiDefinition<?> openApiDefinition) {
        this.addViolation(ruleName, message, openApiDefinition, ViolationType.MANDATORY);
    }

    public void addViolation(String ruleName, String message) {
        this.addViolation(ruleName, message, new Line("", 0), ViolationType.MANDATORY);
    }
}
