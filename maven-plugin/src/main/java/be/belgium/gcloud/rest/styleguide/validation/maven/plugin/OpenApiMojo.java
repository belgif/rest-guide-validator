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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
    List<String> files = new ArrayList<>();

    @Parameter(property = "api-validator.fileWithExclusions")
    List<FileWithExclusion> fileWithExclusions = new ArrayList<>();

    @Parameter(property = "api-validator.skipOnErrors ")
    boolean skipOnErrors = false;

    @Parameter(property = "api-validator.outputTypes")
    List<OutputType> outputTypes ;

    @Parameter(property = "api-validator.outputDir", defaultValue = "target")
    String outputDir = "target";

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject mavenProject;

    private Set<OutputProcessor> outputProcessors;

    private String base;

    private void init(){
        if( files.isEmpty() && fileWithExclusions.isEmpty() )
            throw new IllegalArgumentException("api-validator need at least one file! Add the 'api-validator.files' or 'api-validator.fileWithExclusions' in the plugin configuration.");

        if(mavenProject != null)
            base = this.mavenProject.getBasedir().getAbsolutePath() + File.separator;
        else
            base = System.getProperty("user.dir") + File.separator;
        getLog().debug("Working directory: "+base);

        if ( outputTypes == null )
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

    /**
     * For each file in @files validate using OpenApiValidator.
     * @throws MojoExecutionException when file cannot be read or parse.
     * @throws MojoFailureException when file is not a valid open-api.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();

        AtomicBoolean isValid = new AtomicBoolean(true);

        files.stream()
                .map(filename -> new File(base+filename))
                .forEach(file->{
                    outputProcessors.stream().filter(outputProcessor -> outputProcessor instanceof JUnitOutputProcessor)
                                    .map(o -> (JUnitOutputProcessor)o)
                                    .forEach(jUnitOutputProcessor -> jUnitOutputProcessor.setOutputFile(
                                            new File(outputDir, "TEST-" + file.getName() + ".xml")));

                    isValid.set(OpenApiValidator.isOasValid(file, outputProcessors.toArray(new OutputProcessor[0])) && isValid.get());
                });

        fileWithExclusions.forEach(fileWithExclusion->{
                    File file = new File(base + fileWithExclusion.getFile() );
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
