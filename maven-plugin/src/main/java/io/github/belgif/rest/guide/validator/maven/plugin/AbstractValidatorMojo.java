package io.github.belgif.rest.guide.validator.maven.plugin;

import io.github.belgif.rest.guide.validator.output.OutputGroupBy;
import io.github.belgif.rest.guide.validator.output.OutputProcessor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractValidatorMojo extends AbstractMojo {
    protected static final String FAILURE_MESSAGE = "At least 1 error in validation !";

    @Parameter(property = "rest-guide-validator.files")
    protected List<File> files = new ArrayList<>();

    @Parameter(property = "rest-guide-validator.excludedFiles")
    protected List<String> excludedFiles = new ArrayList<>();

    @Parameter(property = "rest-guide-validator.groupBy", defaultValue = "rule")
    protected String groupBy = "rule";

    /**
     * @deprecated fileWithExclusions parameter is ignored, please use x-ignore-rules in the OpenApi file or excludedFiles in the POM to exclude complete files.
     */
    @Deprecated(since = "1.2.2", forRemoval = true)
    @Parameter(property = "rest-guide-validator.fileWithExclusions")
    List<String> fileWithExclusions = new ArrayList<>();

    /**
     * @deprecated excludeResources parameter is ignored, please use x-ignore-rules in the OpenApi file or excludedFiles in the POM to exclude complete files.
     */
    @Deprecated(since = "1.2.2", forRemoval = true)
    @Parameter(property = "rest-guide-validator.excludeResources")
    List<String> excludeResources = new ArrayList<>();


    protected Set<OutputProcessor> outputProcessors;
    protected final List<File> filesToProcess = new ArrayList<>();
    protected OutputGroupBy outputGroupBy;

    private List<File> getJsonAndYamlFiles(File directory) {
        return getJsonAndYamlFiles(List.of(Objects.requireNonNull(directory.listFiles())));
    }

    private List<File> getJsonAndYamlFiles(List<File> fileList) {
        return fileList.stream().filter(file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml") || file.getName().endsWith(".json")).collect(Collectors.toList());
    }

    protected void init() throws FileNotFoundException {
        outputGroupBy = OutputGroupBy.fromString(groupBy);
        initOutputProcessor();
        initFiles();
    }

    protected abstract void initOutputProcessor();

    /**
     * Throw an IllegalArgumentException if no file is provided or if a file is not in the maven project.
     * Add all yaml or gson file from provided directories.
     */
    private void initFiles() throws FileNotFoundException {
        if (files.isEmpty())
            throw new IllegalArgumentException("rest-guide-validator need at least one file ! Set the 'rest-guide-validator.files' parameter.");
        Optional<File> fileNotFound = files.stream().filter(file -> !file.exists()).findAny();
        if (fileNotFound.isPresent()) {
            throw new FileNotFoundException("File not found: " + fileNotFound.get().getAbsolutePath());
        }

        // replace directories in list by the json and yaml files in them
        var dirs = files.stream().filter(File::isDirectory).collect(Collectors.toSet());
        var filesFromDirs = dirs.stream().flatMap(dir -> getJsonAndYamlFiles(dir).stream()).collect(Collectors.toList());
        var filesInRootFolder = files.stream().filter(File::isFile).filter(file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml") || file.getName().endsWith(".json")).collect(Collectors.toList());

        filesToProcess.addAll(filesInRootFolder);
        filesToProcess.addAll(filesFromDirs);
    }

}
