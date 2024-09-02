package io.github.belgif.rest.guide.validator.core.parser;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    public JsonPointer add(JsonPointer jsonPointer) {
        return new JsonPointer(this.jsonPointer + jsonPointer);
    }

    public static JsonPointer relative(String propertyName) {
        return new JsonPointer("").add(propertyName);
    }

    public static JsonPointer relative(int arrayIndex) {
        return new JsonPointer("").add(arrayIndex);
    }

    public static String escape(String unescaped) {
        return unescaped.replace("~", "~0").replace("/", "~1");
    }

    public static String unescape(String escaped) {
        return escaped.replace("~1", "/").replace("~0", "~");
    }

    public List<String> splitSegments() {
        return Arrays.stream(jsonPointer.split("/")).filter(pointer -> !pointer.isEmpty()).map(JsonPointer::unescape).collect(Collectors.toList());
    }

    public List<String> translate(int oasVersion) {
        var pointers = this.splitSegments();
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
            if ("parameters".equals(pointers.get(0)) && pointers.size() > 2) {
                pointers.remove(pointers.get(2));
            }
        }
        if (oasVersion == 2 && "servers".equals(pointers.get(0))) {
            pointers.set(0, "basePath");
            pointers.remove(1);
        }
        if (oasVersion == 2 && "schema".equals(pointers.get(pointers.size() - 1))) {
            pointers.remove(pointers.size() - 1);
        }
        if (oasVersion == 2 && "paths".equals(pointers.get(0)) && pointers.contains("requestBody")) {
            throw new JsonPointerOas2Exception(toPrettyString());
        }
        return pointers;
    }

    public JsonPointer translateToJsonPointer(int oasVersion) {
        var pointer = new JsonPointer("");
        for (String segment : this.translate(oasVersion)) {
            pointer = pointer.add(segment);
        }
        return pointer;
    }

    @Override
    public String toString() {
        return jsonPointer;
    }

    public String toPrettyString() {
        var sb = new StringBuilder();
        Arrays.stream(jsonPointer.split("/")).filter(pointer -> !pointer.isEmpty()).map(JsonPointer::unescape).forEach(segment -> {
            if (segment.startsWith("/")) {
                segment = segment.substring(1);
            }
            sb.append("/").append(segment);
        });
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonPointer that = (JsonPointer) o;
        return Objects.equals(jsonPointer, that.jsonPointer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(jsonPointer);
    }
}
