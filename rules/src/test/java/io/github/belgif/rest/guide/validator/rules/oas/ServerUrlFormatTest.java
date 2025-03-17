package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

 class ServerUrlFormatTest extends AbstractOasRuleTest {
    @Test
     void testCorrectMinimalBasepath() {
        assertNoViolations(callRules("correctMinimalBasepath.yaml"));
    }
    @Test
     void testCorrectLocalhostBasepath() {
        assertNoViolations(callRules("correctLocalhostBasepath.yaml"));
    }
    @Test
     void testCorrectLocalhostWithPortBasepath() {
        assertNoViolations(callRules("correctLocalhostWithPortBasepath.yaml"));
    }
    @Test
     void testCorrectHttpsLocalhostBasepath() {
        assertNoViolations(callRules("correctHttpsLocalhostBasepath.yaml"));
    }
    @Test
     void testCorrectExternalHostBasepath() {
        assertNoViolations(callRules("correctExternalHostBasepath.yaml"));
    }
    @Test
     void testCorrectExternalHostWithPortBasepath() {
        assertNoViolations(callRules("correctExternalHostWithPortBasepath.yaml"));
    }
    @Test
     void testCorrectPrefixBasepath() {
        assertNoViolations(callRules("correctPrefixBasepath.yaml"));
    }
    @Test
     void testCorrectMultiPrefixBasepath() {
        assertNoViolations(callRules("correctMultiPrefixBasepath.yaml"));
    }
    @Test
     void testViolatingInsecureExternalBasepath() {
        assertViolations(callRules("violatingInsecureExternalBasepath.yaml"));
    }
    @Test
     void testViolatingNoPathAfterHostBasepath() {
        assertViolations(callRules("violatingNoPathAfterHostBasepath.yaml"));
    }
    @Test
     void testViolatingTrailingSlashBasepath() {
        assertViolations(callRules("violatingTrailingSlashBasepath.yaml"));
    }
    @Test
     void testViolatingLowerCamelCaseBasepath() {
        assertViolations(callRules("violatingLowerCamelCaseBasepath.yaml"));
    }
    @Test
     void testViolatingAbsentVersionBasepath() {
        assertViolations(callRules("violatingAbsentVersionBasepath.yaml"));
    }

    @Test
     void testEntryFileWithoutPathsReferencingFileWithPath() {
        assertNoViolations(callRules("entryFileWithoutPaths.yaml"));
    }
}
