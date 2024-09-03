package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.model.helper.MediaType;
import io.github.belgif.rest.guide.validator.core.model.helper.MediaTypeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaTypeTest {

    @Test
    void testIncludes() {
        var mediaType = new MediaType("application/json");
        assertTrue(mediaType.includes(new MediaType("application/problem+json")));
        assertTrue(mediaType.includes(new MediaType("application/json")));
        assertFalse(mediaType.includes(new MediaType("application/problem+xml")));
        assertFalse(mediaType.includes(new MediaType("*/*")));

        mediaType = new MediaType("multipart/*");
        assertTrue(mediaType.includes(new MediaType("multipart/*")));
        assertTrue(mediaType.includes(new MediaType("multipart/form-data")));
        assertTrue(mediaType.includes(new MediaType("multipart/chunked")));

        mediaType = new MediaType("*/*");
        assertTrue(mediaType.includes(new MediaType("*/*")));
        assertTrue(mediaType.includes(new MediaType("application/problem+json")));
        assertTrue(mediaType.includes(new MediaType("application/json")));
        assertTrue(mediaType.includes(new MediaType("application/problem+xml")));
        assertTrue(mediaType.includes(new MediaType("multipart/*")));
        assertTrue(mediaType.includes(new MediaType("multipart/form-data")));
        assertTrue(mediaType.includes(new MediaType("multipart/chunked")));
        assertTrue(mediaType.includes(new MediaType("application/*")));
    }

    @Test
    void testSubTypeSuffix() {
        MediaType mediaType = new MediaType("application/json+");
        assertNull(mediaType.getSubTypeSuffix());
        mediaType = new MediaType("application/problem+json");
        assertEquals("json", mediaType.getSubTypeSuffix());
    }

    @Test
    void testIncompatibleMediaType() {
        assertThrows(MediaTypeException.class, () -> new MediaType("thiswillnotwork"));
        assertThrows(MediaTypeException.class, () -> new MediaType("this/will/not/work"));
        assertThrows(MediaTypeException.class, () -> new MediaType(""));
        assertThrows(MediaTypeException.class, () -> new MediaType(null));
    }

    @Test
    void testCaseInsensitive() {
        var mediaType = new MediaType("APPlication/JSON");
        assertEquals("application", mediaType.getType());
        assertEquals("json", mediaType.getSubType());
    }

    @Test
    void testCharsetInformationIsDropped() {
        var mediaType = new MediaType("application/json;charset=UTF-8");
        assertEquals("application/json", mediaType.toString());
    }

}
