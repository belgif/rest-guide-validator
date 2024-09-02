package io.github.belgif.rest.guide.validator.core.parser;

public class JsonPointerOas2Exception extends RuntimeException {
    public JsonPointerOas2Exception(String jsonPointer) {
        super("JsonPointer: " + jsonPointer + " cannot be translated back to OAS2 due to lack of context.");
    }
}
