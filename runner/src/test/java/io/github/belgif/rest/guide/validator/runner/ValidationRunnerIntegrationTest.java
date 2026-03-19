package io.github.belgif.rest.guide.validator.runner;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationRunnerIntegrationTest {

    /*
    This testcase can be used during development to see if any of the openapi files used in a certain organization give problems to the rest-guide-validator.
     */

    @Test
    @Disabled
    void testFolderWithMultipleOpenApis() {
        List<File> files = findOpenApiFiles("absolute/path/to/folder");
        Map<String, Boolean> reports = new HashMap<>();
        for (File file : files) {
            try {
                ValidationRunner runner = ValidationRunner.builder()
                        .files(List.of(file))
                .excludedFiles(Collections.EMPTY_LIST)
                .groupBy("rule")
                        .build();
                reports.put(file.getAbsolutePath(), runner.executeRules());
            } catch (Exception e) {
                reports.put(file.getAbsolutePath(), null);
                System.out.println("An error occurred for " + file.getAbsolutePath());
            }
        }
        assertEquals(Collections.EMPTY_LIST, reports.entrySet().stream().filter(e -> e.getValue() == null).toList());
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
