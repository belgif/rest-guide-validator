package io.github.belgif.rest.guide.validator.output;

import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.core.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OutputGroupByTest {

    @Test
    void groupByRuleTest() {
        OutputGroupBy outputGroupBy = OutputGroupBy.RULE;
        Map<String, List<Violation>> groupedViolations = outputGroupBy.groupViolations(getViolationCollection());

        // Assert all rules are in separate groups
        assertEquals(6, groupedViolations.keySet().size());

        List<String> keyList = new ArrayList<>(groupedViolations.keySet());
        // Assert correct order
        assertTrue(keyList.get(0).contains("rule1"));
        assertTrue(keyList.get(1).contains("rule1"));
        assertTrue(keyList.get(2).contains("rule2"));
        assertTrue(keyList.get(3).contains("rule3"));
        assertTrue(keyList.get(4).contains("rule4"));
        assertTrue(keyList.get(5).contains("rule1"));
        assertTrue(keyList.get(5).contains("ignored"));
    }

    @Test
    void groupByRuleSubGroupsHaveCorrectOrderTest() {
        OutputGroupBy outputGroupBy = OutputGroupBy.RULE;
        Map<String, List<Violation>> groupedViolations = outputGroupBy.groupViolations(getViolationCollection());
        List<String> keyList = new ArrayList<>(groupedViolations.keySet());
        assertEquals("file2", groupedViolations.get(keyList.get(0)).get(1).getLineNumber().getFileName());
    }

    @Test
    void groupByRuleGroupMessagesAreSetCorrectlyTest() {
        OutputGroupBy outputGroupBy = OutputGroupBy.RULE;
        Map<String, List<Violation>> groupedViolations = outputGroupBy.groupViolations(getViolationCollection());
        List<String> keyList = new ArrayList<>(groupedViolations.keySet());

        assertEquals("[MANDATORY]    rule1        description",keyList.get(0));
        assertEquals("[MANDATORY]    rule1        nope",keyList.get(1));
        assertTrue(keyList.get(keyList.size()-1).contains("IGNORED"));
    }

    @Test
    void groupByRuleViolationReportMessagesAreSetCorrectlyTest() {
        OutputGroupBy outputGroupBy = OutputGroupBy.RULE;
        Map<String, List<Violation>> groupedViolations = outputGroupBy.groupViolations(getViolationCollection());
        List<String> keyList = new ArrayList<>(groupedViolations.keySet());

        assertTrue(keyList.get(0).contains("description"));
        assertTrue(keyList.get(keyList.size()-1).contains("ignored"));
        List<Violation> violations = groupedViolations.get(keyList.get(0));
        assertTrue(violations.get(0).getReportMessage().contains("first message"));
        assertFalse(violations.get(0).getReportMessage().contains("null"));
    }

    @Test
    void groupByFileTest() {
        OutputGroupBy outputGroupBy = OutputGroupBy.FILE;
        Map<String, List<Violation>> groupedViolations = outputGroupBy.groupViolations(getViolationCollection());

        // Assert groups are made correctly and in the correct order
        assertEquals(2, groupedViolations.size());

        List<String> keyList = new ArrayList<>(groupedViolations.keySet());
        assertTrue(keyList.get(0).contains("file1"));
        assertTrue(keyList.get(1).contains("file2"));

        // Assert correct amount of violations in each group
        List<Violation> file1 = groupedViolations.get(keyList.get(0));
        List<Violation> file2 = groupedViolations.get(keyList.get(1));
        assertEquals(4, file1.size());
        assertEquals(4, file2.size());
    }

    @Test
    void groupByFileGroupOrderTest() {
        OutputGroupBy outputGroupBy = OutputGroupBy.FILE;
        Map<String, List<Violation>> groupedViolations = outputGroupBy.groupViolations(getViolationCollection());

        List<String> keyList = new ArrayList<>(groupedViolations.keySet());

        List<Violation> file1 = groupedViolations.get(keyList.get(0));
        List<Violation> file2 = groupedViolations.get(keyList.get(1));

        assertEquals(ViolationType.MANDATORY, file1.get(0).getType());
        assertEquals(ViolationType.IGNORED, file1.get(file1.size()-1).getType());
        assertEquals(ViolationType.MANDATORY, file2.get(0).getType());
        assertEquals(ViolationType.RECOMMENDED, file2.get(file2.size()-1).getType());
    }

    private List<Violation> getViolationCollection() {
        List<Violation> violations = new ArrayList<>();
        Violation fileOneMandatoryOne = new Violation("rule1", "description", "first message", ViolationType.MANDATORY, new Line("file1", 1), "pointer/to");
        Violation fileOneMandatoryTwo = new Violation("rule2", "description", ViolationType.MANDATORY, new Line("file1", 100), "pointer/to");
        Violation fileTwoMandatoryTwo = new Violation("rule2", "description", ViolationType.MANDATORY, new Line("file2", 1), "pointer/to");
        Violation fileOneMandatoryThree = new Violation("rule3", "description", ViolationType.MANDATORY, new Line("file1", 101), "pointer/to");
        Violation fileOneIgnoredOne = new Violation("rule1", "ignored: this is ignored", ViolationType.IGNORED, new Line("file1", 50), "pointer/to");
        Violation fileTwoMandatoryOne = new Violation("rule1", "description", ViolationType.MANDATORY, new Line("file2", 20), "pointer/to");
        Violation fileTwoMandatoryOneDifferentMessage = new Violation("rule1", "nope", ViolationType.MANDATORY, new Line("file2", 12), "pointer/to");
        Violation fileTwoRecommended = new Violation("rule4", "recommended description", ViolationType.RECOMMENDED, new Line("file2", 19), "pointer/to");

        violations.add(fileTwoRecommended);
        violations.add(fileTwoMandatoryOne);
        violations.add(fileOneMandatoryOne);
        violations.add(fileOneIgnoredOne);
        violations.add(fileOneMandatoryTwo);
        violations.add(fileTwoMandatoryTwo);
        violations.add(fileOneMandatoryThree);
        violations.add(fileTwoMandatoryOneDifferentMessage);

        return violations;
    }

}
