package io.github.belgif.rest.guide.validator.cli.options;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorOptionsTest {

    @Test
    void testDefaultValues() {
        ValidatorOptions options = CommandLine.populateCommand(new ValidatorOptions());
        assertNotNull(options.getExcludedFiles());
        assertTrue(options.getExcludedFiles().isEmpty());
        assertEquals("console", options.getOutputTypes().get(0));
        Path outputDir = Paths.get("").toAbsolutePath();
        assertEquals(outputDir, options.getOutputDir());
        File jsonFile = new File(outputDir.toFile(), "validationReport.json");
        assertEquals(jsonFile, options.getJsonOutputFile());
        assertEquals("rule", options.getGroupBy());
    }

    @Test
    void testRelativeJsonOutputFile() {
        ValidatorOptions options = CommandLine.populateCommand(new ValidatorOptions(), "--jsonOutputFile=mySubFolder/myOutput.json", "--outputDir=/myOutputDir");
        Path expectedPath = Paths.get("/myOutputDir");
        assertEquals(expectedPath, options.getOutputDir());
        Path expectedJsonPath = Paths.get("/myOutputDir/mySubFolder/myOutput.json");
        assertEquals(expectedJsonPath, options.getJsonOutputFile().toPath());
    }

    @Test
    void testAbsoluteJsonOutputFile() {
        String userHome = System.getProperty("user.home");
        Path jsonPath = Paths.get(userHome, "myJsonFolder", "myOutput.json");
        ValidatorOptions options = CommandLine.populateCommand(new ValidatorOptions(), "--jsonOutputFile=" + jsonPath, "--outputDir=/myOutputDir");
        Path expectedPath = Paths.get("/myOutputDir");
        assertEquals(expectedPath, options.getOutputDir());
        assertEquals(jsonPath, options.getJsonOutputFile().toPath());
    }
}
