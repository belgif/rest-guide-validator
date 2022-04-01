package be.belgium.gcloud.rest.styleguide.validation.rules;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.Violation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Getter
@Slf4j
public abstract class AbstractRuleTest {
    protected int errorCount = 1;

    protected abstract OpenApiViolationAggregator callRules(String fileName) throws IOException ;

    @Test
    protected void isValidTest() throws IOException {
        var apiDetail = callRules("/swagger.yaml");

       var violations = apiDetail.getViolations().stream().filter(v-> v.getRuleName().equalsIgnoreCase(getRuleName())).collect(Collectors.toSet());
        assertTrue(violations.size() < 1 || apiDetail.getViolations().stream().filter(v-> v.getRuleName().equalsIgnoreCase(getRuleName())).count() < 1  ,
                getMessage(violations));
    }

    @Test
    protected void isInvalidTest() throws IOException {
        var apiDetail = callRules("/swagger_bad.yaml");

        assertFalse(apiDetail.getViolations().size() < 1);
        var violations = apiDetail.getViolations().stream().filter(v-> v.getRuleName().equalsIgnoreCase(getRuleName())).collect(Collectors.toSet());
        assertTrue( (getErrorCount() == 1 && violations.size() > 0) | (getErrorCount() > 1 && violations.size() == getErrorCount() ) ,
                getMessage(violations));
        violations.forEach(v-> log.warn(v.toString()));
    }

    protected String getMessage(Set<Violation> violations){
        return "Number of errors for the rule '"+getRuleName()+"' : "+ violations.size() +"\n"
                + violations.stream().map(Violation::toString).collect(Collectors.joining("\n")) ;
    }

    protected static String lowerCaseFirstStripTestSuffix(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1, s.length() - 4);
    }

    protected String getRuleName(){
        return lowerCaseFirstStripTestSuffix(getClass().getSimpleName());
    }
}
