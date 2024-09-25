package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViolationReportTest {

    @Test
    void violationsWithNonRecursiveIgnoredFilesTest() {
        List<String> excludedFiles = new ArrayList<>();
        excludedFiles.add("schemas/*");

        var oas = new ViolationReport();
        oas.setExcludedFiles(excludedFiles);
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);

        var defIgnored = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/EmployerClasses")).findAny();
        assertTrue(defIgnored.isPresent());
        var defIncluded = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/Problem")).findAny();
        assertTrue(defIncluded.isPresent());

        oas.addViolation("[rule-name]", "This violation should be ignored", "reason", defIgnored.get(), ViolationLevel.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be included", "reason", defIncluded.get(), ViolationLevel.MANDATORY);

        var ignoredViolations = oas.getIgnoredViolations();
        var includedViolations = oas.getActionableViolations();

        assertEquals(1, ignoredViolations.size());
        assertEquals(1, includedViolations.size());

        assertEquals("employer.yaml", ignoredViolations.get(0).getLineNumber().getFileName());
        assertEquals("problem-v1.yaml", includedViolations.get(0).getLineNumber().getFileName());
    }

    @Test
    void violationsWithRecursiveIgnoredFilesTest() {
        List<String> excludedFiles = new ArrayList<>();
        excludedFiles.add("schemas/**");

        var oas = new ViolationReport();
        oas.setExcludedFiles(excludedFiles);
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);

        var employerSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/EmployerClasses")).findAny();
        assertTrue(employerSchema.isPresent());
        var problemSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/Problem")).findAny();
        assertTrue(problemSchema.isPresent());

        oas.addViolation("[rule-name]", "This violation should be ignored", "reason", employerSchema.get(), ViolationLevel.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be ignored", "reason",problemSchema.get(), ViolationLevel.MANDATORY);

        var ignoredViolations = oas.getIgnoredViolations();
        var includedViolations = oas.getActionableViolations();

        assertEquals(2, ignoredViolations.size());
        assertEquals(0, includedViolations.size());

        assertEquals("employer.yaml", ignoredViolations.get(0).getLineNumber().getFileName());
        assertEquals("problem-v1.yaml", ignoredViolations.get(1).getLineNumber().getFileName());
    }

    @Test
    void violationsWithIgnoredFilesWildcardTest() {
        List<String> excludedFiles = new ArrayList<>();
        excludedFiles.add("**.yaml");

        var oas = new ViolationReport();
        oas.setExcludedFiles(excludedFiles);
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);

        var employerSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/EmployerClasses")).findAny();
        assertTrue(employerSchema.isPresent());
        var logoSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/LogoMetaData")).findAny();
        assertTrue(logoSchema.isPresent());
        var logoPath = parserResult.getPathDefinitions().stream().filter(path -> path.getJsonPointer().toString().equals("/paths/~1logos")).findAny();
        assertTrue(logoPath.isPresent());

        oas.addViolation("[rule-name]", "This violation should be ignored", "reason", employerSchema.get(), ViolationLevel.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be ignored", "reason", logoSchema.get(), ViolationLevel.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be ignored", "reason", logoPath.get(), ViolationLevel.MANDATORY);

        var ignoredViolations = oas.getIgnoredViolations();
        var includedViolations = oas.getActionableViolations();

        assertEquals(3, ignoredViolations.size());
        assertEquals(0, includedViolations.size());

        assertEquals("employer.yaml", ignoredViolations.get(0).getLineNumber().getFileName());
        assertEquals("logo.yaml", ignoredViolations.get(1).getLineNumber().getFileName());
        assertEquals("openapi.yaml", ignoredViolations.get(2).getLineNumber().getFileName());

    }

    @Test
    void violationsWithTwoIgnoredFilesTest() {
        List<String> excludedFiles = new ArrayList<>();
        excludedFiles.add("../logo.yaml");
        excludedFiles.add("schemas/belgif/**");

        var oas = new ViolationReport();
        oas.setExcludedFiles(excludedFiles);
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);

        var logoSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/LogoMetaData")).findAny();
        assertTrue(logoSchema.isPresent());
        var problemSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/Problem")).findAny();
        assertTrue(problemSchema.isPresent());

        oas.addViolation("[rule-name]", "This violation should be ignored", "reason", logoSchema.get(), ViolationLevel.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be ignored", "reason", problemSchema.get(), ViolationLevel.MANDATORY);

        var ignoredViolations = oas.getIgnoredViolations();
        var includedViolations = oas.getActionableViolations();

        assertEquals(2, ignoredViolations.size());
        assertEquals(0, includedViolations.size());

        assertEquals("logo.yaml", ignoredViolations.get(0).getLineNumber().getFileName());
        assertEquals("problem-v1.yaml", ignoredViolations.get(1).getLineNumber().getFileName());
    }

    @Test
    void multipleViolationsInOneIgnoredFileOnlyHaveOneViolationInOutputTest() {
        List<String> excludedFiles = new ArrayList<>();
        excludedFiles.add("../logo.yaml");

        var oas = new ViolationReport();
        oas.setExcludedFiles(excludedFiles);
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);

        var logoMetaSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/LogoMetaData")).findAny();
        assertTrue(logoMetaSchema.isPresent());
        var logoSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/LogoMetaData")).findAny();
        assertTrue(logoSchema.isPresent());

        oas.addViolation("[rule-name]", "This violation should be ignored", "reason", logoMetaSchema.get(), ViolationLevel.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be ignored", "reason", logoSchema.get(), ViolationLevel.MANDATORY);

        assertEquals(0, oas.getActionableViolations().size());
        assertEquals(1, oas.getIgnoredViolations().size());
    }

}
