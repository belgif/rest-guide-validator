package io.github.belgif.rest.guide.validator.cli.util;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class VersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        return new String[]{getMavenVersion()};
    }

    private String getMavenVersion() {
        try (InputStream input = VersionProvider.class.getClassLoader().getResourceAsStream("io/github/belgif/rest/guide/validator/util/version.properties")) {
            Properties prop = new Properties();
            if (input != null) {
                prop.load(input);
                return prop.getProperty("validator.version");
            }
        } catch (Exception ex) {
            log.error("Unable to fetch version", ex);
        }
        return "unknown";
    }

}
