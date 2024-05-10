package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

public class ServerUrlFormatTest extends AbstractOasRuleTest {
    @Test
    public void testCorrectMinimalBasepath() {
        assertNoViolations(callRules("correctMinimalBasepath.yaml"));
    }
    @Test
    public void testCorrectLocalhostBasepath() {
        assertNoViolations(callRules("correctLocalhostBasepath.yaml"));
    }
    @Test
    public void testCorrectLocalhostWithPortBasepath() {
        assertNoViolations(callRules("correctLocalhostWithPortBasepath.yaml"));
    }
    @Test
    public void testCorrectHttpsLocalhostBasepath() {
        assertNoViolations(callRules("correctHttpsLocalhostBasepath.yaml"));
    }
    @Test
    public void testCorrectExternalHostBasepath() {
        assertNoViolations(callRules("correctExternalHostBasepath.yaml"));
    }
    @Test
    public void testCorrectExternalHostWithPortBasepath() {
        assertNoViolations(callRules("correctExternalHostWithPortBasepath.yaml"));
    }
    @Test
    public void testCorrectPrefixBasepath() {
        assertNoViolations(callRules("correctPrefixBasepath.yaml"));
    }
    @Test
    public void testCorrectMultiPrefixBasepath() {
        assertNoViolations(callRules("correctMultiPrefixBasepath.yaml"));
    }
    @Test
    public void testViolatingInsecureExternalBasepath() {
        assertViolations(callRules("violatingInsecureExternalBasepath.yaml"));
    }
    @Test
    public void testViolatingNoPathAfterHostBasepath() {
        assertViolations(callRules("violatingNoPathAfterHostBasepath.yaml"));
    }
    @Test
    public void testViolatingTrailingSlashBasepath() {
        assertViolations(callRules("violatingTrailingSlashBasepath.yaml"));
    }
    @Test
    public void testViolatingLowerCamelCaseBasepath() {
        assertViolations(callRules("violatingLowerCamelCaseBasepath.yaml"));
    }
    @Test
    public void testViolatingAbsentVersionBasepath() {
        assertViolations(callRules("violatingAbsentVersionBasepath.yaml"));
    }
}
