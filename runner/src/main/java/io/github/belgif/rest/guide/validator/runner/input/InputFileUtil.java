package io.github.belgif.rest.guide.validator.runner.input;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class InputFileUtil {

    private InputFileUtil() {
    }

    public static List<File> getJsonAndYamlFiles(File directory) {
        return getJsonAndYamlFiles(List.of(Objects.requireNonNull(directory.listFiles())));
    }

    public static List<File> getJsonAndYamlFiles(List<File> fileList) {
        return fileList.stream().filter(file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml") || file.getName().endsWith(".json")).map(File::getAbsoluteFile).toList();
    }
}
