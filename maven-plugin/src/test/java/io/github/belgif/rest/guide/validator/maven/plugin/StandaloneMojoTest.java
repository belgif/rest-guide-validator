package io.github.belgif.rest.guide.validator.maven.plugin;

public class StandaloneMojoTest extends AbstractValidatorMojoTest {
    @Override
    protected StandaloneMojo getMojo() {
        return new StandaloneMojo();
    }
}
