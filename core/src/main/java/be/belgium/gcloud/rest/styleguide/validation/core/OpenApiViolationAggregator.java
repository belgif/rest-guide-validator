package be.belgium.gcloud.rest.styleguide.validation.core;

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
public class OpenApiViolationAggregator {

    private File openApiFile;
    private final List<Violation> violations = new ArrayList<>();
    protected List<String> src;

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
    public void addViolation( String ruleName, String message){
        this.addViolation( ruleName, message,0, ViolationType.MANDATORY);
    }

    public int getLineNumber(String predicat){
        if(predicat == null)
            return 0;
        for(var i=0; i< src.size(); i++){
            if (src.get(i).contains(predicat))
                return i+1; // line start at 1
        }
        return 0;
    }
    public int getLineNumber(int start, String predicat){
        if(predicat == null)
            return 0;
        for(var i=start; i< src.size(); i++){
            if (src.get(i).contains(predicat))
                return i+1; // line start at 1
        }
        return 0;
    }
}
