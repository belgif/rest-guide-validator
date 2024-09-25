package io.github.belgif.rest.guide.validator.rules;

import io.github.belgif.rest.guide.validator.OpenApiSingleRuleValidator;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.Violation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Getter
@Slf4j
public abstract class AbstractOasRuleTest {

    protected ViolationReport callRules(String fileName)  {
        String ruleFileName = getRulesFile(getClass().getSimpleName());
        var ruleUrl = getClass().getResource(ruleFileName);
        if (ruleUrl == null) {
            throw new RuntimeException("Could not find file " + ruleFileName);
        }
        var openApiFile = new File(getClass().getResource(lowerCaseFirstStripTestSuffix(getClass().getSimpleName()) + "/" + fileName).getFile());
        var validator = new OpenApiSingleRuleValidator(ruleUrl.getFile());
        return validator.isOasValid(openApiFile, null);
    }

    public static void assertNoViolations(ViolationReport validationResult) {
        assertEquals(0, validationResult.getActionableViolations().size(), getMessage(validationResult));
    }

    public static void assertViolations(ViolationReport validationResult) {
        assertNotEquals(0, validationResult.getActionableViolations().size(), getMessage(validationResult));
    }

    public static String getMessage(ViolationReport validationResult){
        var violations = validationResult.getActionableViolations();
        return "Number of errors : "+ violations.size() +"\n"
                + violations.stream().map(Violation::toString).collect(Collectors.joining("\n")) ;
    }

    public static void assertErrorCount(int expectedErrors, ViolationReport validationResult) {
        assertEquals(expectedErrors,validationResult.getActionableViolations().size(), getMessage(validationResult));
    }

    private String getRulesFile(String s) {
        return "Rule-" + s.substring(0, s.length() - 4) + ".drl";
    }

    private static String lowerCaseFirstStripTestSuffix(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1, s.length() - 4);
    }

}
