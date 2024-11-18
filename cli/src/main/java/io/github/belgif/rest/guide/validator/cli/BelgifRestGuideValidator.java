package io.github.belgif.rest.guide.validator.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Scanner;

public class BelgifRestGuideValidator {

    private static final Logger log = LoggerFactory.getLogger(BelgifRestGuideValidator.class);

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BelgifRestGuideCli()).execute(args);
        log.info("\nPress Enter to exit...");
        new Scanner(System.in).nextLine();
        System.exit(exitCode);
    }

}
