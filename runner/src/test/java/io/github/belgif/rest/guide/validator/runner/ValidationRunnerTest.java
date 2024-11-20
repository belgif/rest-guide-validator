package io.github.belgif.rest.guide.validator.runner;

import io.github.belgif.rest.guide.validator.runner.output.OutputType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ValidationRunnerTest {

    @Test
    void testListOptionsOutputDir() throws IOException {
        File file = Files.createTempFile("tmp", "tmp").toFile();
        List<File> files = new ArrayList<>();
        files.add(file);
        List<OutputType> outputTypes = new ArrayList<>();
        outputTypes.add(OutputType.CONSOLE);
        ValidationRunner runner = new ValidationRunner(files, null, outputTypes, null, null, null);
        assertFalse(runner.listOptions().contains("OutputDir"));
    }

}
