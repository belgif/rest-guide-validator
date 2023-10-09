package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.core.model.OpenApiDefinition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    protected List<String> src;

    private int ruleNumber;
    private float time;

    public void addViolation(Violation violation){
        this.violations.add(violation);
    }
    public void addViolation(String ruleName, String message, int lineNumber, ViolationType type){
        this.violations.add(Violation.builder()
                .ruleName(ruleName)
                .message(message)
                .lineNumber(lineNumber)
                .type(type).build());
    }
    public void addViolation( String ruleName, String message, int lineNumber){
        this.addViolation( ruleName, message, lineNumber, ViolationType.MANDATORY);
    }
    public void addViolation(String ruleName, String message, OpenApiDefinition<?> openApiDefinition){
        int lineNumber = openApiDefinition.getLineNumber(this);
        this.addViolation( ruleName, message + "\t"+openApiDefinition.getJsonPointer(), lineNumber, ViolationType.MANDATORY);
    }
    public void addViolation( String ruleName, String message){
        this.addViolation( ruleName, message,0, ViolationType.MANDATORY);
    }

    /**
     * Return the first line number for the predicate in the src.
     * @param predicate String to find
     * @return the line number or 0 if not found
     */
    public int getLineNumber(String predicate){
        // TODO: after, refactoring, move line number logic to other class. Maybe create something like a RuleContext object to pass around instead
        if(predicate == null)
            return 0;
        for(var i=0; i< src.size(); i++){
            if (src.get(i).contains(predicate))
                return i+1; // line start at 1
        }
        return 0;
    }

    /**
     * Return the first line number for the predicate in the src but start a line 'start'.
     * @param start the line number to start the research.
     * @param predicate String to find
     * @return the line number or 0 if not found
     */
    public int getLineNumber(int start, String predicate){
        if(predicate == null)
            return 0;
        for(var i=start; i< src.size(); i++){
            if (src.get(i).contains(predicate))
                return i+1; // line start at 1
        }
        //todo: Does not work with multifiles
        return start;
    }
}
