package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircularReferenceUtilTest {

    @Test
    void testCircularReferenceViaDiscriminator() {
        assertParsingInvalid("invalidViaDiscriminator");
    }

    @Test
    void testCircularReferenceViaDirectRef() {
        assertParsingInvalid("invalidViaDirectRef");
    }

    @Test
    void testCircularReferenceViaOneOf() {
        assertParsingInvalid("invalidViaOneOf");
    }

    @Test
    void testCircularReferenceViaAllOf() {
        assertParsingInvalid("invalidViaAllOf");
    }

    @Test
    void testCircularReferenceValidViaProperty() {
        assertParsingValid("validViaProperty");
    }

    @Test
    void testCircularReferenceValidViaAllOfInheritance() {
        assertParsingValid("validViaAllOfInheritance");
    }

    private void assertParsingValid(String fileName) {
        assertDoesNotThrow(() -> new Parser(getFile(fileName)).parse(new ViolationReport()));
    }

    private void assertParsingInvalid(String fileName) {
        ViolationReport violationReport = new ViolationReport();
        File file = getFile(fileName);
        Parser parser = new Parser(file);
        assertThrows(RuntimeException.class, () -> parser.parse(violationReport));
    }

    private File getFile(String fileName) {
        return new File(Objects.requireNonNull(getClass().getResource("../rules/circularReferences/" + fileName + ".yaml")).getFile());
    }

}
