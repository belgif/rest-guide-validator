package io.github.belgif.rest.guide.validator.runner.input;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputFileUtilTest {

    @Test
    void testJsonAndYamlFilesFromList() {
        File yamlFile1 = new File("myFirstYamlFile.yaml");
        File jsonFile1 = new File("myFirstJsonFile.json");
        File yamlFile2 = new File("mySecondYamlFile.yaml");
        File jsonFile2 = new File("mySecondJsonFile.json");
        File otherFile = new File("myOtherFile.other");
        List<File> files = new ArrayList<>();
        files.add(yamlFile1);
        files.add(jsonFile1);
        files.add(yamlFile2);
        files.add(jsonFile2);
        files.add(otherFile);

        List<File> filesToProcess = InputFileUtil.getJsonAndYamlFiles(files);
        assertTrue(filesToProcess.contains(yamlFile1.getAbsoluteFile()));
        assertTrue(filesToProcess.contains(yamlFile2.getAbsoluteFile()));
        assertTrue(filesToProcess.contains(jsonFile1.getAbsoluteFile()));
        assertTrue(filesToProcess.contains(jsonFile2.getAbsoluteFile()));
        assertFalse(filesToProcess.contains(otherFile.getAbsoluteFile()));
    }

    @Test
    void testJsonAndYamlFilesFromDirectory() throws IOException {
        Path directory = Files.createTempDirectory("tmp");
        Path yamlFile1 = Files.createFile(directory.resolve("myFirstYamlFile.yaml"));
        Path jsonFile1 = Files.createFile(directory.resolve("myFirstJsonFile.json"));
        Path yamlFile2 = Files.createFile(directory.resolve("mySecondYamlFile.yaml"));
        Path jsonFile2 = Files.createFile(directory.resolve("mySecondJsonFile.json"));
        Path otherFile = Files.createFile(directory.resolve("myOtherFile.other"));
        List<Path> files = new ArrayList<>();
        files.add(yamlFile1);
        files.add(jsonFile1);
        files.add(yamlFile2);
        files.add(jsonFile2);
        files.add(otherFile);

        List<File> filesToProcess = InputFileUtil.getJsonAndYamlFiles(directory.toFile());
        assertTrue(filesToProcess.contains(yamlFile1.toFile().getAbsoluteFile()));
        assertTrue(filesToProcess.contains(yamlFile2.toFile().getAbsoluteFile()));
        assertTrue(filesToProcess.contains(jsonFile1.toFile().getAbsoluteFile()));
        assertTrue(filesToProcess.contains(jsonFile2.toFile().getAbsoluteFile()));
        assertFalse(filesToProcess.contains(otherFile.toFile().getAbsoluteFile()));
    }

}
