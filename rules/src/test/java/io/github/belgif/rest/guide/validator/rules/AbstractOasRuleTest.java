package io.github.belgif.rest.guide.validator.rules;

import io.github.belgif.rest.guide.validator.OpenApiSingleRuleValidator;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.Violation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
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
        return validator.callRule(openApiFile, null);
    }

    /*
    This method can be used during development to test a certain rule against multiple openapi files. Will search folders recursively.
    Expects an absolute file path to the folder
     */
    protected Map<String, ViolationReport> callRulesOnFolder(String folder) {
        String ruleFileName = getRulesFile(getClass().getSimpleName());
        var ruleUrl = getClass().getResource(ruleFileName);
        if (ruleUrl == null) {
            throw new RuntimeException("Could not find file " + ruleFileName);
        }
        var validator = new OpenApiSingleRuleValidator(ruleUrl.getFile());
        List<File> openapiFiles = findOpenApiFiles(folder);
        log.info("Found {} openapi files", openapiFiles.size());
        Map<String, ViolationReport> reports = new HashMap<>();
        for (File openapi : openapiFiles) {
            try {
                reports.put(openapi.getAbsolutePath(), validator.callRule(openapi, null));
            } catch (Exception e) {
                log.error("File: {} got an exception with the validator", openapi.getAbsolutePath());
                reports.put(openapi.getAbsolutePath(), null);
            }
        }
        log.info("{} reports were made", reports.size());
        log.info("There were {} files with violations on this rule.", reports.entrySet().stream().filter(e -> !e.getValue().isOasValid()).count());
        log.info("{} files were compliant with this rule", reports.entrySet().stream().filter(e -> e.getValue().isOasValid()).count());
        return reports;
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

    private static List<File> findOpenApiFiles(String folder) {
        List<File> openApiFiles = new ArrayList<>();
        File baseFolder = new File(folder);
        for (File child : Objects.requireNonNull(baseFolder.listFiles())) {
            if (child.isDirectory()) {
                openApiFiles.addAll(findOpenApiFiles(child.getAbsolutePath()));
            }
            if (child.getName().contains("openapi.yaml")) {
                openApiFiles.add(child);
            }
        }
        return openApiFiles;
    }

}
