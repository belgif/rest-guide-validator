package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.core.model.OpenApiDefinition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
/**
 * Structure that hold the violations.
 */
public class OpenApiViolationAggregator {

    private File openApiFile;
    private final List<Violation> violations = new ArrayList<>();
    protected Map<String, List<String>> src;

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
        Line lineNumber = openApiDefinition.getLineNumber(this);
        this.addViolation(ruleName, message + "\t" + openApiDefinition.getJsonPointer().toPrettyString(), lineNumber, violationType);
    }

    public void addViolation(String ruleName, String message, OpenApiDefinition<?> openApiDefinition) {
        this.addViolation(ruleName, message, openApiDefinition, ViolationType.MANDATORY);
    }

    //TODO remove
//    private static String resolveJsonPointer(String jsonPointer) {
//        // ~1 is used to represent / character in jsonPointer
//        String modifiedString = jsonPointer;
//        if (jsonPointer.contains("~1")) {
//            modifiedString = jsonPointer.replaceAll("~1", "/");
//        }
//        if (modifiedString.contains("//")) {
//            modifiedString = modifiedString.replaceAll("//", "/");
//        }
//        return modifiedString;
//    }

    public void addViolation(String ruleName, String message) {
        this.addViolation(ruleName, message, new Line(openApiFile.getName(), 0), ViolationType.MANDATORY);
    }

    /**
     * Return the first line number for the predicate in the src.
     *
     * @param predicate String to find
     * @return the line number or 0 if not found
     */
    public Line getLineNumber(String predicate) {
        if (predicate == null)
            return new Line(openApiFile.getName(), 0);
        for (String file : src.keySet()) {
            for (var i = 0; i < src.get(file).size(); i++) {
                if (src.get(file).get(i).contains(predicate))
                    return new Line(file, i + 1); // line start at 1
            }
        }
        return new Line(openApiFile.getName(), 0);
    }

    public Line getLineNumber(String fileName, String predicate) {
        if (predicate == null)
            return null;
        for (var i = 0; i < src.get(fileName).size(); i++) {
            if (src.get(fileName).get(i).contains(predicate))
                return new Line(fileName, i + 1); // line start at 1
        }

        return null;
    }

    /**
     * Return the first line number for the predicate in the src but start a line 'start'.
     *
     * @param start     the line number to start the research.
     * @param predicate String to find
     * @return the line number or 0 if not found
     */
    public Line getLineNumber(Line start, String predicate) {
        if (predicate == null)
            return start;
        for (var i = start.getLineNumber(); i < src.get(start.getFileName()).size(); i++) {
            if (src.get(start.getFileName()).get(i).contains(predicate))
                return new Line(start.getFileName(), i + 1); // line start at 1
        }
        return start;
    }
}
