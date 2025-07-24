package io.github.belgif.rest.guide.validator.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Scanner;

public class BelgifRestGuideValidator {

    private static final Logger log = LoggerFactory.getLogger(BelgifRestGuideValidator.class);

    public static void main(String[] args) {
        int exitCode = 1;
        try {
            exitCode = new CommandLine(new BelgifRestGuideCli()).execute(args);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            try (Scanner sc = new Scanner(System.in)) {
                log.info("\nPress Enter to exit...");
                sc.nextLine();
            }
            System.exit(exitCode);
        }
    }

}
