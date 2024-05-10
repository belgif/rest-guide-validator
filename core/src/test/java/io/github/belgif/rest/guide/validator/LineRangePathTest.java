package io.github.belgif.rest.guide.validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineRangePathTest {

    @Test
    void compareTo() {
        var a = new LineRangePath("a", 1);
        var b = new LineRangePath("b", 5);
        assertTrue(a.compareTo(b) < 0);
    }

    @Test
    void inRange() {
        var a = new LineRangePath("a", 1);
        a.setEnd(10);
        assertTrue(a.inRange(5));
        assertTrue(a.inRange(1));
        assertTrue(a.inRange(10));
        assertFalse(a.inRange(11));
    }
}