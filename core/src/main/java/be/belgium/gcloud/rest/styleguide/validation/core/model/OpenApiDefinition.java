package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.LineRangePath;
import be.belgium.gcloud.rest.styleguide.validation.core.Line;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                if (expectedPath == null || !ref.contains(expectedPath)) {
                    if (expectedPath != null) {
                        log.error(getFullyQualifiedPointer() + "/$ref: '{}' is not of correct type (expected a component in \"{}\")",ref, expectedPath.equals("/components/schemas") && this.result.getOasVersion() == 2 ? "/definitions" : expectedPath);
                        this.result.setParsingValid(false);
                    } else {
                        log.warn("[Internal error] Use of $ref is not supported by validator for type {} ({}).", this.getModel().getClass().getName(), ref);
                    }
                }
            }
        }
    }

    private String getExpectedRefPath() {
        if (model instanceof Parameter) {
            return this.result.oasVersion == 2 ? "/parameters" : "/components/parameters";
        } else if (model instanceof RequestBody) {
            return this.result.oasVersion == 2 ? "/parameters" : "/components/requestBodies";
        } else if (model instanceof APIResponse) {
            return this.result.oasVersion == 2 ? "/responses" : "/components/responses";
        } else if (model instanceof Schema) {
            // OpenApi Parser sets refs to definitions in oas2 to components/schemas as well
            return "/components/schemas";
        } else if (model instanceof PathItem) {
            return "/paths";
        } else if (model instanceof Header) {
            return this.result.oasVersion == 2 ? null : "/components/headers";
        } else if (model instanceof Example) {
            return this.result.oasVersion == 2 ? "examples" : "/components/examples";
        } else if (model instanceof Callback) {
            return this.result.oasVersion == 2 ? null : "/components/callbacks";
        } else if (model instanceof Link) {
            return this.result.oasVersion == 2 ? null : "/components/links";
        } else if (model instanceof SecurityScheme) {
            return this.result.oasVersion == 2 ? "/securityDefinitions" : "/components/securitySchemes";
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
        OpenApiDefinition<?> indirectParent = this.parent;
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
        Line startLine = getLineNumber();
        LineRangePath range = new LineRangePath(identifier, startLine.getLineNumber());
        range.setEnd(getEndLineNumber(openApiFile));
        return range;
    }

    private Line getTopLevelLineNumber() {
        if (definitionType == DefinitionType.TOP_LEVEL) {
            List<String> pointers = handleJsonPointer();
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
        return searchObjectInFile(getTopLevelParent().openApiFile, handleJsonPointer(), true);
    }

    private List<String> handleJsonPointer() {
        List<String> pointers = jsonPointer.splitSegments();
        if (pointers.isEmpty()) {
            log.warn("Invalid location for definition: " + jsonPointer);
            return null;
        }
        if (result.getOasVersion() == 2 && "components".equals(pointers.get(0))) {
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
        if (result.getOasVersion() == 2 && "servers".equals(pointers.get(0))) {
            pointers.set(0, "basePath");
            pointers.remove(1);
        }
        return pointers;
    }

    private Line searchObjectInFile(File file, List<String> pointers, boolean approximate) {
        JsonFactory factory;
        if (result.getSrc().get(file.getAbsolutePath()).isYaml()) {
            factory = new YAMLFactory();
        } else {
            factory = new JsonFactory();
        }

        try {
            JsonParser jsonParser = factory.createParser(result.getSrc().get(file.getAbsolutePath()).getSrc());
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
            if (currentNestingLevel == wantedNestingLevel && token == JsonToken.FIELD_NAME && !inArray) {
                if (pointers.get(0).equals(jsonParser.getCurrentName())) {
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
            }
            if (currentNestingLevel == wantedNestingLevel && token == JsonToken.START_OBJECT) {
                if (pointers.get(0).equals(arrayIndex + "")) {
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

        List<String> pointers = handleJsonPointer();
        try {
            JsonParser jsonParser = factory.createParser(result.getSrc().get(file.getAbsolutePath()).getSrc());
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
            if (currentNestingLevel == wantedNestingLevel && token == JsonToken.FIELD_NAME && !inArray) {
                if (!objectFound && pointers.get(0).equals(jsonParser.getCurrentName())) {
                    pointers.remove(0);
                    wantedNestingLevel++;
                    if (jsonParser.getCurrentLocation() != null) {
                        if (pointers.isEmpty()) {
                            objectFound = true;
                        }
                    }
                    continue;
                }
            }
            if (currentNestingLevel == wantedNestingLevel && token == JsonToken.START_OBJECT) {
                if (!objectFound && pointers.get(0).equals(arrayIndex + "")) {
                    pointers.remove(0);
                    wantedNestingLevel++;
                    inArray = false;
                    arrayIndex = 0;
                    currentNestingLevel++;
                    if (jsonParser.getCurrentLocation() != null) {
                        if (pointers.isEmpty()) {
                            objectFound = true;
                        }
                    }
                    continue;
                }
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
        Object ignoreObj = extensions.get("x-ignore-rules");
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
        return this.getOpenApiFile().getName()+"#"+this.getJsonPointer().toPrettyString();
    }

}
