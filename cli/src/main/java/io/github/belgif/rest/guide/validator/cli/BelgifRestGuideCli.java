package io.github.belgif.rest.guide.validator.cli;

import io.github.belgif.rest.guide.validator.cli.options.ValidatorOptions;
import picocli.CommandLine;


@CommandLine.Command(name = "belgif-validate-openapi", mixinStandardHelpOptions = true, showDefaultValues = true)
public class BelgifRestGuideCli implements Runnable {

    @CommandLine.Mixin
    private ValidatorOptions options;

    @Override
    public void run() {
        System.out.println("Belgif Rest Guide Cli started");

        System.out.println(options.getFiles());
        System.out.println(options.getExcludedFiles());
        System.out.println(options.getOutputTypes());
        System.out.println(options.getOutputDir());
        System.out.println(options.getJsonOutputFile());
        System.out.println(options.getGroupBy());
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BelgifRestGuideCli()).execute(args);
        System.exit(exitCode);
    }
}
