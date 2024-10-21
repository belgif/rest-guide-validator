package io.github.belgif.rest.guide.validator.cli;

import io.github.belgif.rest.guide.validator.cli.options.ValidatorOptions;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "belgif-validate-openapi", mixinStandardHelpOptions = true)
public class BelgifRestGuideCli implements Runnable {

    @CommandLine.Mixin
    private ValidatorOptions options;

    @CommandLine.Option(names = {"-f", "--files"}, description = "Files", defaultValue = "default")
    public List<String> files;

    @Override
    public void run() {
        if (files == null || files.isEmpty()) {
            System.exit(1);
        }
        int count = 0;
        for (String file : files) {
            count ++;
            System.out.println("file <<" + count + ">> : " + file);
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BelgifRestGuideCli()).execute(args);
        System.exit(exitCode);
    }
}
