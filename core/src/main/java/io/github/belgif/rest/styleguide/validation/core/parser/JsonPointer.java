package io.github.belgif.rest.styleguide.validation.core.parser;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class JsonPointer {
    private final String jsonPointer;

    public JsonPointer(@Nonnull String jsonPointer) {
        if (jsonPointer.endsWith("/")) {
            this.jsonPointer = jsonPointer.substring(0, jsonPointer.length() - 1);
        } else {
            this.jsonPointer = jsonPointer;
        }
    }

    /**
     * @param propertyName property (unescaped) to add to JsonPointer
     * @return new JsonPointer with property added
     */
    public JsonPointer add(String propertyName) {
        if (propertyName != null) {
            return new JsonPointer(this.jsonPointer + "/" + escape(propertyName));
        }
        return this;
    }

    public JsonPointer add(int arrayIndex) {
        return new JsonPointer(this.jsonPointer + "/" + arrayIndex);
    }

    public JsonPointer add (JsonPointer jsonPointer) {
        return new JsonPointer(this.jsonPointer+jsonPointer);
    }

    public static JsonPointer relative(String propertyName) {
        return new JsonPointer("").add(propertyName);
    }

    public static JsonPointer relative(int arrayIndex) {
        return new JsonPointer("").add(arrayIndex);
    }

    public static String escape(String unescaped) {
        return unescaped.replaceAll("~", "~0").replaceAll("/", "~1");
    }

    public static String unescape(String escaped) {
        return escaped.replaceAll("~1", "/").replaceAll("~0", "~");
    }

    public List<String> splitSegments() {
        return Arrays.stream(jsonPointer.split("/")).filter(pointer -> !pointer.isEmpty()).map(JsonPointer::unescape).collect(Collectors.toList());
    }

    public List<String> translate(int oasVersion) {
        List<String> pointers = this.splitSegments();
        if (pointers.isEmpty()) {
            log.warn("Invalid location for definition: " + jsonPointer);
            return null;
        }
        if (oasVersion == 2 && "components".equals(pointers.get(0))) {
            pointers.remove(0);
            if ("schemas".equals(pointers.get(0))) {
                pointers.set(0, "definitions");
            }
            if ("requestBodies".equals(pointers.get(0))) {
                pointers.set(0, "parameters");
            }
            if ("securitySchemes".equals(pointers.get(0))) {
                pointers.set(0, "securityDefinitions");
            }
        }
        if (oasVersion == 2 && "servers".equals(pointers.get(0))) {
            pointers.set(0, "basePath");
            pointers.remove(1);
        }
        return pointers;
    }

    @Override
    public String toString() {
        return jsonPointer;
    }

    public String toPrettyString() {
        StringBuilder sb= new StringBuilder();
        Arrays.stream(jsonPointer.split("/")).filter(pointer -> !pointer.isEmpty()).map(JsonPointer::unescape).forEach(segment -> {
            if(segment.startsWith("/")) {
                segment = segment.substring(1);
            }
            sb.append("/").append(segment);
        });
        return sb.toString();
    }

}
