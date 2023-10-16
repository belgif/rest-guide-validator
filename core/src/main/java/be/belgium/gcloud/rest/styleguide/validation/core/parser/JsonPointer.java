package be.belgium.gcloud.rest.styleguide.validation.core.parser;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class JsonPointer {
    //TODO implement
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
        return new JsonPointer(this.jsonPointer + "/" + escape(propertyName));
    }

    public JsonPointer add(int arrayIndex) {
        return new JsonPointer(this.jsonPointer + "/" + arrayIndex);
    }

    public static String escape(String unescaped) {
        // replace all ~ with ~0 and / by ~1
        throw new RuntimeException("TODO");
    }

    public static String unescape(String escaped) {
        throw new RuntimeException("TODO");
    }

    public List<String> splitSegments() {
        return Arrays.stream(jsonPointer.split("/")).filter(pointer -> !pointer.isEmpty()).map(JsonPointer::unescape).collect(Collectors.toList());
    }
}
