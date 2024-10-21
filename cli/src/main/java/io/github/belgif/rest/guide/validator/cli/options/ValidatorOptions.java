package io.github.belgif.rest.guide.validator.cli.options;

import io.github.belgif.rest.guide.validator.cli.util.VersionProvider;
import picocli.CommandLine;

@CommandLine.Command(synopsisHeading      = "%nUsage:%n%n",
        descriptionHeading   = "%nDescription:%n%n",
        parameterListHeading = "%nParameters:%n%n",
        optionListHeading    = "%nOptions:%n%n",
        commandListHeading   = "%nCommands:%n%n",
        versionProvider = VersionProvider.class)
public class ValidatorOptions {
}
