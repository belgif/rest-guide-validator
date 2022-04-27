package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import be.belgium.gcloud.rest.styleguide.validation.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Maven plugin that check if a Swagger API or an open API is conformed the G-Cloud standards.
 * The plugin use the following parameters:
 *  - api-validator.files: a list of files to validate
 *  - api-validator.outputType: the output processor to process the violation. @see OutputType. Default is Console.
 *  - api-validator.outputDir: the directory to write the XML Junit files. Only relevant for the OutputType.JUNIT
 *  - ${project} root directory for api-validator.files
 */
@Mojo(name = "api-validator", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class OpenApiMojo extends AbstractMojo {

    static final String FAILURE_MESSAGE = "At least 1 error in validation !";

    @Parameter(property = "api-validator.files")
    List<File> files = new ArrayList<>();

    @Parameter(property = "api-validator.fileWithExclusions")
    List<FileWithExclusion> fileWithExclusions = new ArrayList<>();

    @Parameter(property = "api-validator.skipOnErrors ")
    boolean skipOnErrors = false;

    @Parameter(property = "api-validator.outputTypes")
    List<OutputType> outputTypes ;

    @Parameter(property = "api-validator.outputDir", defaultValue = "target")
    String outputDir = "target";

    @Parameter(readonly = true, defaultValue = "${project}")
    MavenProject mavenProject;

    @Parameter(property = "api-validator.excludeResources")
    List<String> excludeResources = new ArrayList<>();

    private Set<OutputProcessor> outputProcessors;

    private List<File> addAllFiles(File directory){
        return List.of(directory.listFiles((dir, name) ->
                name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".json")));
    }
    private void init(){
       initFiles();
       initOutputProcessor();
       addExclusions();
    }

    private void initOutputProcessor(){
        if ( outputTypes == null || outputTypes.isEmpty())
            outputProcessors = Set.of(new ConsoleOutputProcessor[]{new ConsoleOutputProcessor()});
        else {
            outputProcessors = new HashSet<>();
            outputTypes.forEach(outputType -> {
                switch (outputType) {
                    case NONE: break;
                    case JUNIT:
                        outputProcessors.add(new JUnitOutputProcessor()); break;
                    case LOG4J:
                        outputProcessors.add(new Log4JOutputProcessor()); break;
                    default:
                        outputProcessors.add(new ConsoleOutputProcessor());
                }
            });
        }
    }

    private void initFiles(){
        if( files.isEmpty() && fileWithExclusions.isEmpty() )
            throw new IllegalArgumentException("api-validator need at least one file ! Add the 'api-validator.files' or 'api-validator.fileWithExclusions' in the plugin configuration.");
        if( files.stream().anyMatch(file -> file.getPath().startsWith(mavenProject.getFile().getPath())) ||
            fileWithExclusions.stream().anyMatch(f -> f.getFile().getPath().startsWith(mavenProject.getFile().getPath())) )
            throw new IllegalArgumentException("All files must be in the maven project structure !");

        // add all file from directories
        var dirs = files.stream().filter(file -> file.isDirectory()).collect(Collectors.toSet());
        var fromDir = dirs.stream().map(dir->addAllFiles(dir)).collect(Collectors.toList()).stream().collect(Collectors.toList());

        dirs.forEach(dir ->files.remove(dir));
        fromDir.forEach(list-> files.addAll(list));
    }

    private void addExclusions(){
        fileWithExclusions.forEach(fileWithExclusion -> fileWithExclusion.getExcludesPaths().addAll(excludeResources));
        fileWithExclusions.addAll( files.stream()
                .map(file -> new FileWithExclusion(file, excludeResources))
                .collect(Collectors.toList()) );
    }
    /**
     * For each file in @files validate using OpenApiValidator.
     * @throws MojoExecutionException when file cannot be read or parse.
     * @throws MojoFailureException when file is not a valid open-api.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();

        AtomicBoolean isValid = new AtomicBoolean(true);
        fileWithExclusions.forEach(fileWithExclusion->{
                    var file = fileWithExclusion.getFile() ;
                    outputProcessors.stream().filter(outputProcessor -> outputProcessor instanceof JUnitOutputProcessor)
                            .map(o -> (JUnitOutputProcessor)o)
                            .forEach(jUnitOutputProcessor -> jUnitOutputProcessor.setOutputFile(
                                    new File(outputDir, "TEST-" + file.getName() + ".xml")));

                    isValid.set(OpenApiValidator.isOasValid(file, fileWithExclusion.getExcludesPaths(), outputProcessors.toArray(new OutputProcessor[0])) && isValid.get());
                });

        if (! skipOnErrors && ! isValid.get())
            throw new MojoFailureException(FAILURE_MESSAGE);
    }
}
