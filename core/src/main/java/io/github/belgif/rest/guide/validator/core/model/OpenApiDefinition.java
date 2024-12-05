package io.github.belgif.rest.guide.validator.core.model;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.belgif.rest.guide.validator.LineRangePath;
import io.github.belgif.rest.guide.validator.core.Line;
import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.JsonPointerOas2Exception;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.Reference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.belgif.rest.guide.validator.core.constant.ExpectedReferencePathConstants.OAS_2_LOCATIONS;
import static io.github.belgif.rest.guide.validator.core.constant.ExpectedReferencePathConstants.OAS_3_LOCATIONS;

@Slf4j
@Getter
public abstract class OpenApiDefinition<T extends Constructible> {
    private final Parser.ParserResult result;
    private final T model;
    protected final DefinitionType definitionType;

    private OpenApiDefinition<?> parent; //when definitionType is INLINE

    private final String identifier; // mandatory when definitionType is TOP_LEVEL, optional otherwise

    private final File openApiFile;
    private final JsonPointer jsonPointer; //Due to parser used, jsonPointer always points to equivalent locations in OAS 3 spec for OAS 2.0 documents. Conversion to OAS2 locations is handled by getExpectedRefPath() and handleJsonPointer()

    /**
     * Key: Name of the ignored rule.
     * Value: Reason why the rule is ignored.
     */
    private final Map<String, String> ignoredRules;

    /**
     * Constructor for an inline definition
     */
    protected OpenApiDefinition(T model, OpenApiDefinition<?> parent, String identifier, JsonPointer relativeJsonPointer) {
        this.result = parent.getResult();
        this.model = model;
        this.definitionType = DefinitionType.INLINE;
        this.parent = parent;
        this.identifier = identifier;
        this.openApiFile = parent.getOpenApiFile();
        this.jsonPointer = parent.getJsonPointer().add(relativeJsonPointer);
        this.ignoredRules = parseIgnoredRules();
        checkRef();
    }

    /**
     * Constructor for a definition under components
     */
    protected OpenApiDefinition(T model, String identifier, File openApiFile, JsonPointer jsonPointer, Parser.ParserResult result) {
        this.result = result;
        this.model = model;
        this.definitionType = DefinitionType.TOP_LEVEL;
        this.identifier = identifier;
        this.openApiFile = openApiFile;
        this.jsonPointer = jsonPointer;
        this.ignoredRules = parseIgnoredRules();
        checkRef();
    }

    private void checkRef() {
        if (this.getModel() instanceof Reference) {
            String ref = ((Reference<?>) model).getRef();
            if (ref != null && !ref.isEmpty()) {
                String expectedPath = getExpectedRefPath();
                if (expectedPath == null) {
                    log.warn("[Internal error] Use of $ref is not supported by validator for type {} ({}).", this.getModel().getClass().getName(), ref);
                    return;
                }
                if (!ref.contains(expectedPath)) {
                    if (!ref.startsWith("#") && expectedPath.equals("/components/schemas") && this.result.getOasVersion() == 2 && ref.contains("/definitions")) {
                        return;
                    }
                    log.error("{}/$ref: '{}' is not of correct type (expected a component in \"{}\")", getFullyQualifiedPointer(), ref, expectedPath.equals("/components/schemas") && this.result.getOasVersion() == 2 ? "/definitions" : expectedPath);
                    this.result.setParsingValid(false);

                }
            }
        }
    }


    private String getExpectedRefPath() {
        Map<Class<?>, String> versionedRefs = this.result.oasVersion == 2 ? OAS_2_LOCATIONS : OAS_3_LOCATIONS;
        for (Map.Entry<Class<?>, String> entry : versionedRefs.entrySet()) {
            if (entry.getKey().isInstance(model)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public enum DefinitionType {
        INLINE, TOP_LEVEL
    }

    public File getOpenApiFile() {
        if (definitionType == DefinitionType.INLINE) {
            return parent.getOpenApiFile();
        } else {
            return this.openApiFile;
        }
    }

    public String getEffectiveIdentifier() {
        if (this.getIdentifier() == null) {
            return parent.getEffectiveIdentifier();
        } else {
            return identifier;
        }
    }

    public OpenApiDefinition<?> getTopLevelParent() {
        var indirectParent = this.parent;
        while (indirectParent.definitionType == DefinitionType.INLINE) {
            indirectParent = indirectParent.getParent();
        }
        return indirectParent;
    }

    public Line getLineNumber() {
        // Searches for lineNumber, in some cases the JsonPointer points further than is in the actual file (because some refs are resolved automatically in the parser)
        // So for inline objects, it searches as far as it can.
        if (definitionType == DefinitionType.INLINE) {
            return getInlineLineNumber();
        } else {
            return getTopLevelLineNumber();
        }
    }

    public LineRangePath getLineRangePath() {
        var startLine = getLineNumber();
        var range = new LineRangePath(identifier, startLine.getLineNumber());
        range.setEnd(getEndLineNumber(openApiFile));
        return range;
    }

    public JsonPointer getSrcVersionedJsonPointer() {
        return this.jsonPointer.translateToJsonPointer(this.result.getOasVersion());
    }

    /**
     * @return String of JsonPointer, when translation to OAS2 fails, it will return the OAS3 JsonPointer string.
     */
    public String getPrintableJsonPointer() {
        try {
            return getSrcVersionedJsonPointer().toPrettyString();
        } catch (JsonPointerOas2Exception e) {
            log.debug("Ignoring JsonPointer exception for violation printing purposes.", e);
            return this.jsonPointer.toPrettyString() + " (approximate pointer in OpenAPI 3.0 equivalent)";
        }
    }

    private Line getTopLevelLineNumber() {
        if (definitionType == DefinitionType.TOP_LEVEL) {
            List<String> pointers = this.jsonPointer.translate(result.getOasVersion());
            Line line = searchObjectInFile(this.openApiFile, pointers, false);
            if (line != null) {
                return line;
            }
            log.warn("No correct line number found for: " + jsonPointer);
            return new Line(openApiFile.getName(), 0);
        } else {
            return getTopLevelParent().getTopLevelLineNumber();
        }
    }

    private Line getInlineLineNumber() {
        JsonPointer pointer;
        try {
            pointer = getSrcVersionedJsonPointer();
        } catch (JsonPointerOas2Exception e) {
            log.debug("Catched JsonPointerException, ignored for lineNumber calculation.", e);
            pointer = this.jsonPointer;
        }
        return searchObjectInFile(getTopLevelParent().openApiFile, pointer.splitSegments(), true);
    }

    private Line searchObjectInFile(File file, List<String> pointers, boolean approximate) {
        JsonFactory factory;
        if (result.getSrc().get(file.getAbsolutePath()).isYaml()) {
            factory = new YAMLFactory();
        } else {
            factory = new JsonFactory();
        }

        try {
            var jsonParser = factory.createParser(result.getSrc().get(file.getAbsolutePath()).getSrc());
            int ln = followPointers(pointers, jsonParser, approximate);
            if (ln != -1) {
                return new Line(file.getName(), ln);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not parse " + file.getName() + " for linenumber calculation", ex);
        }
        return null;
    }

    // This method will return the lineNumber of the closest Object to the JsonPointer, if for example it points to a String value in an array,
    // It will return the lineNumber of the parent object of the StringValue.
    // If approximate is set to false, it will return an error if the pointer cannot be found completely in the JsonParser.
    private int followPointers(List<String> pointers, JsonParser jsonParser, boolean approximate) throws IOException {
        int lnNumberSoFar = -1;
        int currentNestingLevel = 0;
        int wantedNestingLevel = 1;
        int arrayIndex = 0;
        boolean inArray = false;
        while (!jsonParser.isClosed()) {
            jsonParser.nextToken();
            var token = jsonParser.getCurrentToken();
            if (currentNestingLevel == wantedNestingLevel && token == JsonToken.FIELD_NAME && !inArray && pointers.get(0).equals(jsonParser.getCurrentName())) {
                pointers.remove(0);
                wantedNestingLevel++;
                if (jsonParser.getCurrentLocation() != null) {
                    lnNumberSoFar = jsonParser.getCurrentLocation().getLineNr();
                    if (pointers.isEmpty()) {
                        return jsonParser.getCurrentLocation().getLineNr();
                    }
                }
                continue;
            }

            if (currentNestingLevel == wantedNestingLevel && token == JsonToken.START_OBJECT && pointers.get(0).equals(arrayIndex + "")) {
                pointers.remove(0);
                wantedNestingLevel++;
                inArray = false;
                arrayIndex = 0;
                currentNestingLevel++;
                if (jsonParser.getCurrentLocation() != null) {
                    lnNumberSoFar = jsonParser.getCurrentLocation().getLineNr();
                    if (pointers.isEmpty()) {
                        return jsonParser.getCurrentLocation().getLineNr();
                    }
                }
                continue;
            }

            if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                if (token == JsonToken.START_OBJECT && inArray && currentNestingLevel == wantedNestingLevel) {
                    arrayIndex++;
                }
                currentNestingLevel++;
                if (token == JsonToken.START_ARRAY && currentNestingLevel == wantedNestingLevel) {
                    inArray = true;
                }
            }
            if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                currentNestingLevel--;
                if (token == JsonToken.END_ARRAY && currentNestingLevel == wantedNestingLevel) {
                    arrayIndex = 0;
                    inArray = false;
                }
            }
            if (currentNestingLevel < wantedNestingLevel) {
                break;
            }
        }
        if (approximate) {
            return lnNumberSoFar;
        } else {
            log.info("LineNumber for " + jsonPointer + " might not be correct!");
            return -1;
        }
    }

    private int getEndLineNumber(File file) {
        JsonFactory factory;
        if (result.getSrc().get(file.getAbsolutePath()).isYaml()) {
            factory = new YAMLFactory();
        } else {
            factory = new JsonFactory();
        }

        var pointers = this.jsonPointer.translate(result.getOasVersion());
        try {
            var jsonParser = factory.createParser(result.getSrc().get(file.getAbsolutePath()).getSrc());
            int ln = followPointersEndOfObject(pointers, jsonParser);
            if (ln != -1) {
                return ln;
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not parse " + file.getName() + " for linenumber calculation", ex);
        }
        return 0;
    }

    private int followPointersEndOfObject(List<String> pointers, JsonParser jsonParser) throws IOException {
        int currentNestingLevel = 0;
        int wantedNestingLevel = 1;
        int arrayIndex = 0;
        boolean inArray = false;
        boolean objectFound = false;
        while (!jsonParser.isClosed()) {
            jsonParser.nextToken();
            var token = jsonParser.getCurrentToken();
            if (currentNestingLevel == wantedNestingLevel && token == JsonToken.FIELD_NAME && !inArray && !objectFound && pointers.get(0).equals(jsonParser.getCurrentName())) {
                pointers.remove(0);
                wantedNestingLevel++;
                if (jsonParser.getCurrentLocation() != null && pointers.isEmpty()) {
                    objectFound = true;
                }

                continue;
            }

            if (currentNestingLevel == wantedNestingLevel && token == JsonToken.START_OBJECT && !objectFound && pointers.get(0).equals(arrayIndex + "")) {
                pointers.remove(0);
                wantedNestingLevel++;
                inArray = false;
                arrayIndex = 0;
                currentNestingLevel++;
                if (jsonParser.getCurrentLocation() != null && pointers.isEmpty()) {
                    objectFound = true;
                }

                continue;
            }

            if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                if (token == JsonToken.START_OBJECT && inArray && currentNestingLevel == wantedNestingLevel) {
                    arrayIndex++;
                }
                currentNestingLevel++;
                if (token == JsonToken.START_ARRAY && currentNestingLevel == wantedNestingLevel) {
                    inArray = true;
                }
            }
            if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                if (objectFound && currentNestingLevel == wantedNestingLevel) {
                    return jsonParser.getCurrentLocation().getLineNr();
                }
                currentNestingLevel--;
                if (token == JsonToken.END_ARRAY && currentNestingLevel == wantedNestingLevel) {
                    arrayIndex = 0;
                    inArray = false;
                }
            }
            if (currentNestingLevel < wantedNestingLevel) {
                break;
            }
        }
        log.info("LineNumber for end of " + jsonPointer + " might not be correct!");
        return -1;
    }

    private Map<String, String> parseIgnoredRules() {
        if (!(model instanceof Extensible<?>)) {
            return new HashMap<>();
        }
        Map<String, Object> extensions;
        extensions = ((Extensible<?>) model).getExtensions();
        if (extensions == null || !extensions.containsKey("x-ignore-rules")) {
            return new HashMap<>();
        }
        var ignoreObj = extensions.get("x-ignore-rules");
        if (ignoreObj instanceof Map) {
            Map<String, String> resultMap = new HashMap<>();
            Map<?, ?> ignored = (Map<?, ?>) ignoreObj;
            for (Object key : ignored.keySet()) {
                if (key instanceof String) {
                    String k = (String) key;
                    if (ignored.get(key) instanceof String) {
                        String v = (String) ignored.get(key);
                        resultMap.put(k, v);
                    } else {
                        log.error("Value of {} in x-ignored-rules for {} not of type String", k, jsonPointer.toPrettyString());
                    }
                } else {
                    log.error("Property in x-ignored-rules for {} not of type String", jsonPointer.toPrettyString());
                }
            }
            return resultMap;
        } else {
            log.error("x-ignored-rules for {} not of type Object with rules/reasons as keys and values", jsonPointer.toPrettyString());
            return new HashMap<>();
        }
    }

    public String getFullyQualifiedPointer() {
        return this.getOpenApiFile().getName() + "#" + this.getJsonPointer().toPrettyString();
    }

}
