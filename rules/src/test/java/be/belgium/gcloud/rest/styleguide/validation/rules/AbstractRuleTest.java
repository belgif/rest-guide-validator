package be.belgium.gcloud.rest.styleguide.validation.rules;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.Violation;
import be.belgium.gcloud.rest.styleguide.validation.core.ViolationType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Getter
@Slf4j
public abstract class AbstractRuleTest {
    protected int errorCount = 1;
    protected String ruleName = null;
    protected ViolationType violationType = null;

    protected abstract OpenApiViolationAggregator callRules(String fileName) throws IOException ;

    @Test
    protected void isValidTest() throws IOException {
        var apiDetail = callRules("/swagger.yaml");
        String name = getRuleName();
       var violations = apiDetail.getViolations().stream().filter(v-> v.getRuleName().equalsIgnoreCase(getRuleName()) && (violationType == null || v.type == violationType)).collect(Collectors.toList());
        assertTrue(violations.size() < 1 || apiDetail.getViolations().stream().filter(v-> v.getRuleName().equalsIgnoreCase(getRuleName())).count() < 1  ,
                getMessage(violations));
    }

    @Test
    protected void isInvalidTest() throws IOException {
        var apiDetail = callRules("/swagger_bad.yaml");
        assertFalse(apiDetail.getViolations().size() < 1);
        String name = getRuleName();
        var violations = apiDetail.getViolations().stream().filter(v-> v.getRuleName().equalsIgnoreCase(getRuleName()) && (violationType == null || v.type == violationType)).collect(Collectors.toList());
        assertTrue( (getErrorCount() == 1 && violations.size() > 0) | (getErrorCount() > 1 && violations.size() == getErrorCount() ) ,
                getMessage(violations));
        //violations.forEach(v-> log.warn(v.toString()));
    }

    protected String getMessage(List<Violation> violations){
        return "Number of errors for the rule '"+getRuleName()+"' : "+ violations.size() +"\n"
                + violations.stream().map(Violation::toString).collect(Collectors.joining("\n")) ;
    }

    protected static String lowerCaseFirstStripTestSuffix(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1, s.length() - 4);
    }

    protected String getRuleName(){
        return ((ruleName != null) ? ruleName : lowerCaseFirstStripTestSuffix(getClass().getSimpleName()));
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public void setViolationType(ViolationType violationType) {
        this.violationType = violationType;
    }
}
