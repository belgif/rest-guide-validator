package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class JsonPointerTest {

    @Test
    void escapeTest() {
        assertEquals("~0~1~01~00~0~0", JsonPointer.escape("~/~1~0~~"));
        assertEquals("~/~1~0~~", JsonPointer.unescape(JsonPointer.escape("~/~1~0~~")));
    }

    @Test
    void buildPointerTest() {
        var pointer = new JsonPointer("/paths/").add("/this/is/my/~path/").add("responses/");
        var pointerSegtments = pointer.splitSegments();
        assertEquals("paths", pointerSegtments.get(0));
        assertEquals("/this/is/my/~path/", pointerSegtments.get(1));
        assertEquals("responses/", pointerSegtments.get(2));
        assertEquals("/paths/~1this~1is~1my~1~0path~1/responses~1", pointer.toString());
        assertEquals("/paths/this/is/my/~path//responses/", pointer.toPrettyString());
    }

    @Test
    void addTest() {
        var pointer = new JsonPointer("/first/second");
        assertEquals("/first/second", pointer.toString());
        pointer = new JsonPointer("").add("third").add("fourth").add("fifth/and/sixth/");
        assertEquals("/third/fourth/fifth~1and~1sixth~1", pointer.toString());
        pointer = new JsonPointer("/first/second");
        var secondPointer = new JsonPointer("/third/fourth");
        pointer = pointer.add(secondPointer);
        assertEquals("/first/second/third/fourth", pointer.toString());
    }
}