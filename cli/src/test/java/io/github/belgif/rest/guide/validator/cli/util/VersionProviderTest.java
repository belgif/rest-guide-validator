package io.github.belgif.rest.guide.validator.cli.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class VersionProviderTest {

    @Test
    void testGetVersion() {
        String version = VersionProvider.getValidatorVersion();
        assertNotEquals("unknown", version);
        String regex = "(\\d+?)\\.(\\d+?)\\.(\\d+?)";
        if (version.matches(regex)) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(version);
            assertTrue(matcher.find());
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            assertTrue(major >= 2);
            assertTrue(minor >= 1);
        } else {
            assertEquals("latest", version);
        }
    }

}
