package io.github.belgif.rest.guide.validator.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ViolationTest {

    @Test
    void compareViolationsTest() {
        List<Violation> violations = new ArrayList<>();
        Violation fileOneMandatoryOne = new Violation(null, "my description", ViolationLevel.MANDATORY, new Line("file1", 1), null);
        Violation fileOneMandatoryTwo = new Violation(null, "my description", ViolationLevel.MANDATORY, new Line("file1", 100), null);
        Violation fileOneIgnoredOne = new Violation(null, "my description", ViolationLevel.IGNORED, new Line("file1", 50), null);
        Violation fileTwoMandatoryOne = new Violation(null, "my description", ViolationLevel.MANDATORY, new Line("file2", 20), null);

        violations.add(fileTwoMandatoryOne);
        violations.add(fileOneMandatoryOne);
        violations.add(fileOneIgnoredOne);
        violations.add(fileOneMandatoryTwo);

        List<Violation> sortedViolations = violations.stream().sorted().collect(Collectors.toList());

        assertEquals(sortedViolations.get(0), fileOneMandatoryOne);
        assertEquals(sortedViolations.get(1), fileOneMandatoryTwo);
        assertEquals(sortedViolations.get(2), fileTwoMandatoryOne);
        assertEquals(sortedViolations.get(3), fileOneIgnoredOne);
    }

    @Test
    void descriptionCannotBeNullOrEmptyTest() {
        var line = new Line("file1", 1);
        assertThrows(IllegalArgumentException.class, () -> new Violation(null, null, ViolationLevel.MANDATORY, line, null));
        assertThrows(IllegalArgumentException.class, () -> new Violation(null, " ", ViolationLevel.MANDATORY, line, null));
    }
}
