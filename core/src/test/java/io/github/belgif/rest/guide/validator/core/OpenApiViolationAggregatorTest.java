package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.model.PathDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class OpenApiViolationAggregatorTest {

    @Test
    void violationsWithNonRecursiveIgnoredFilesTest() {
        List<String> excludedFiles = new ArrayList<>();
        excludedFiles.add("schemas/*");

        var oas = new OpenApiViolationAggregator();
        oas.setExcludedFiles(excludedFiles);
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);

        Optional<SchemaDefinition> defIgnored = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/EmployerClasses")).findAny();
        assertTrue(defIgnored.isPresent());
        Optional<SchemaDefinition> defIncluded = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/Problem")).findAny();
        assertTrue(defIncluded.isPresent());

        oas.addViolation("[rule-name]", "This violation should be ignored", defIgnored.get(), ViolationType.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be included", defIncluded.get(), ViolationType.MANDATORY);

        List<Violation> ignoredViolations = oas.getViolations().stream().filter(violation -> violation.type.equals(ViolationType.IGNORED)).collect(Collectors.toList());
        List<Violation> includedViolations = oas.getViolations().stream().filter(violation -> !violation.type.equals(ViolationType.IGNORED)).collect(Collectors.toList());

        assertEquals(1, ignoredViolations.size());
        assertEquals(1, includedViolations.size());

        assertEquals("employer.yaml", ignoredViolations.get(0).getLineNumber().getFileName());
        assertEquals("problem-v1.yaml", includedViolations.get(0).getLineNumber().getFileName());
    }

    @Test
    void violationsWithRecursiveIgnoredFilesTest() {
        List<String> excludedFiles = new ArrayList<>();
        excludedFiles.add("schemas/**");

        var oas = new OpenApiViolationAggregator();
        oas.setExcludedFiles(excludedFiles);
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);

        Optional<SchemaDefinition> employerSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/EmployerClasses")).findAny();
        assertTrue(employerSchema.isPresent());
        Optional<SchemaDefinition> problemSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/Problem")).findAny();
        assertTrue(problemSchema.isPresent());

        oas.addViolation("[rule-name]", "This violation should be ignored", employerSchema.get(), ViolationType.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be ignored", problemSchema.get(), ViolationType.MANDATORY);

        List<Violation> ignoredViolations = oas.getViolations().stream().filter(violation -> violation.type.equals(ViolationType.IGNORED)).collect(Collectors.toList());
        List<Violation> includedViolations = oas.getViolations().stream().filter(violation -> !violation.type.equals(ViolationType.IGNORED)).collect(Collectors.toList());

        assertEquals(2, ignoredViolations.size());
        assertEquals(0, includedViolations.size());

        assertEquals("employer.yaml", ignoredViolations.get(0).getLineNumber().getFileName());
        assertEquals("problem-v1.yaml", ignoredViolations.get(1).getLineNumber().getFileName());
    }

    @Test
    void violationsWithIgnoredFilesWildcardTest() {
        List<String> excludedFiles = new ArrayList<>();
        excludedFiles.add("**.yaml");

        var oas = new OpenApiViolationAggregator();
        oas.setExcludedFiles(excludedFiles);
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);

        Optional<SchemaDefinition> employerSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/EmployerClasses")).findAny();
        assertTrue(employerSchema.isPresent());
        Optional<SchemaDefinition> logoSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/LogoMetaData")).findAny();
        assertTrue(logoSchema.isPresent());
        Optional<PathDefinition> logoPath = parserResult.getPathDefinitions().stream().filter(path -> path.getJsonPointer().toString().equals("/paths/~1logos")).findAny();
        assertTrue(logoPath.isPresent());

        oas.addViolation("[rule-name]", "This violation should be ignored", employerSchema.get(), ViolationType.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be ignored", logoSchema.get(), ViolationType.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be ignored", logoPath.get(), ViolationType.MANDATORY);

        List<Violation> ignoredViolations = oas.getViolations().stream().filter(violation -> violation.type.equals(ViolationType.IGNORED)).collect(Collectors.toList());
        List<Violation> includedViolations = oas.getViolations().stream().filter(violation -> !violation.type.equals(ViolationType.IGNORED)).collect(Collectors.toList());

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

        var oas = new OpenApiViolationAggregator();
        oas.setExcludedFiles(excludedFiles);
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);

        Optional<SchemaDefinition> logoSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/LogoMetaData")).findAny();
        assertTrue(logoSchema.isPresent());
        Optional<SchemaDefinition> problemSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/Problem")).findAny();
        assertTrue(problemSchema.isPresent());

        oas.addViolation("[rule-name]", "This violation should be ignored", logoSchema.get(), ViolationType.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be ignored", problemSchema.get(), ViolationType.MANDATORY);

        List<Violation> ignoredViolations = oas.getViolations().stream().filter(violation -> violation.type.equals(ViolationType.IGNORED)).collect(Collectors.toList());
        List<Violation> includedViolations = oas.getViolations().stream().filter(violation -> !violation.type.equals(ViolationType.IGNORED)).collect(Collectors.toList());

        assertEquals(2, ignoredViolations.size());
        assertEquals(0, includedViolations.size());

        assertEquals("logo.yaml", ignoredViolations.get(0).getLineNumber().getFileName());
        assertEquals("problem-v1.yaml", ignoredViolations.get(1).getLineNumber().getFileName());
    }

    @Test
    void multipleViolationsInOneIgnoredFileOnlyHaveOneViolationInOutputTest() {
        List<String> excludedFiles = new ArrayList<>();
        excludedFiles.add("../logo.yaml");

        var oas = new OpenApiViolationAggregator();
        oas.setExcludedFiles(excludedFiles);
        var file = new File(getClass().getResource("../rules/referencedFiles/openapi.yaml").getFile());
        var parserResult = new Parser(file).parse(oas);

        Optional<SchemaDefinition> logoMetaSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/LogoMetaData")).findAny();
        assertTrue(logoMetaSchema.isPresent());
        Optional<SchemaDefinition> logoSchema = parserResult.getSchemas().stream().filter(schema -> schema.getJsonPointer().toString().equals("/components/schemas/LogoMetaData")).findAny();
        assertTrue(logoSchema.isPresent());

        oas.addViolation("[rule-name]", "This violation should be ignored", logoMetaSchema.get(), ViolationType.MANDATORY);
        oas.addViolation("[rule-name]", "This violation should be ignored", logoSchema.get(), ViolationType.MANDATORY);


        assertEquals(1, oas.getViolations().size());
        assertEquals(ViolationType.IGNORED, oas.getViolations().get(0).type);
    }

}
