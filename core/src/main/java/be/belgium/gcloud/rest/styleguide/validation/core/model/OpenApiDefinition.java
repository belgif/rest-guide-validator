package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.Line;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.Constructible;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Getter
public abstract class OpenApiDefinition<T extends Constructible> {
    private final T model;
    protected final DefinitionType definitionType;

    private OpenApiDefinition<?> parent; //when definitionType is INLINE

    private final String identifier; // mandatory when definitionType is TOP_LEVEL, optional otherwise

    private final File openApiFile;
    private final JsonPointer jsonPointer;

    /**
     * Constructor for an inline definition
     */
    protected OpenApiDefinition(T model, OpenApiDefinition<?> parent, String identifier, String relativeJsonPointer) {
        this.model = model;
        this.definitionType = DefinitionType.INLINE;
        this.parent = parent;
        this.identifier = identifier;
        this.openApiFile = parent.getOpenApiFile();
        this.jsonPointer = new JsonPointer(parent.getJsonPointer() + relativeJsonPointer);
    }

    protected OpenApiDefinition(T model, OpenApiDefinition<?> parent, String identifier, JsonPointer relativeJsonPointer) {
        this.model = model;
        this.definitionType = DefinitionType.INLINE;
        this.parent = parent;
        this.identifier = identifier;
        this.openApiFile = parent.getOpenApiFile();
        this.jsonPointer = parent.getJsonPointer().add(relativeJsonPointer);
    }

    /**
     * Constructor for a definition under components
     */
    protected OpenApiDefinition(T model, String identifier, File openApiFile, String jsonPointer) {
        this.model = model;
        this.definitionType = DefinitionType.TOP_LEVEL;
        this.identifier = identifier;
        this.openApiFile = openApiFile;
        this.jsonPointer = new JsonPointer(jsonPointer);
    }

    protected OpenApiDefinition(T model, String identifier, File openApiFile, JsonPointer jsonPointer) {
        this.model = model;
        this.definitionType = DefinitionType.TOP_LEVEL;
        this.identifier = identifier;
        this.openApiFile = openApiFile;
        this.jsonPointer = jsonPointer;
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

    public OpenApiDefinition<?> getTopLevelParent() {
        OpenApiDefinition<?> indirectParent = this.parent;
        while (indirectParent.definitionType == DefinitionType.INLINE) {
            indirectParent = indirectParent.getParent();
        }
        return indirectParent;
    }

    public Line getLineNumber(OpenApiViolationAggregator aggregator) {
        // Searches for lineNumber, in some cases the JsonPointer points further than is in the actual file (because some refs are resolved automatically in the parser)
        // So for inline objects, it searches as far as it can.
        if (definitionType == DefinitionType.INLINE) {
            return getInlineLineNumber(aggregator);
        } else {
            return getTopLevelLineNumber(aggregator);
        }
    }

    private Line getTopLevelLineNumber(OpenApiViolationAggregator aggregator) {
        if (definitionType == DefinitionType.TOP_LEVEL) {
            for (String fileName : aggregator.getSrc().keySet()) {
                List<String> pointers = handleJsonPointer(aggregator);
                if (pointers == null) {
                    return new Line(openApiFile.getName(), 0);
                }
                Line line = searchObjectInFile(fileName, aggregator, pointers, false);
                if (line != null) {
                    return line;
                }
            }
            log.warn("No correct line number found for: " + jsonPointer);
            return new Line(openApiFile.getName(), 0);
        } else {
            return getTopLevelParent().getTopLevelLineNumber(aggregator);
        }
    }

    private Line getInlineLineNumber(OpenApiViolationAggregator aggregator) {
        Line lineNumber = getTopLevelParent().getLineNumber(aggregator);
        lineNumber = searchObjectInFile(lineNumber.getFileName(), aggregator, handleJsonPointer(aggregator), true);
        return lineNumber;
    }

    private List<String> handleJsonPointer(OpenApiViolationAggregator aggregator) {
        List<String> pointers = jsonPointer.splitSegments();
        if (pointers.isEmpty()) {
            log.warn("Invalid location for definition: " + jsonPointer);
            return null;
        }
        if (getSrcVersion(aggregator) == 2 && "components".equals(pointers.get(0))) {
            pointers.remove(0);
            if ("schemas".equals(pointers.get(0))) {
                pointers.set(0, "definitions");
            }
        }
        if (getSrcVersion(aggregator) == 2 && "servers".equals(pointers.get(0))) {
            pointers.set(0, "basePath");
            pointers.remove(1);
        }
        return pointers;
    }

    private Line searchObjectInFile(String fileName, OpenApiViolationAggregator aggregator, List<String> pointers, boolean approximate) {
        JsonFactory factory;
        if (isYaml(aggregator)) {
            factory = new YAMLFactory();
        } else {
            factory = new JsonFactory();
        }

        //TODO: Once old lineNumber calculation is completely fased out, we can store complete String in src map instead of a list of strings
        StringBuilder sb = new StringBuilder();
        for (String ln : aggregator.getSrc().get(fileName)) {
            sb.append(ln).append("\n");
        }

        try {
            JsonParser jsonParser = factory.createParser(sb.toString());
            int ln = followPointers(pointers, jsonParser, approximate);
            if (ln != -1) {
                return new Line(fileName, ln);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not parse " + fileName + " for linenumber calculation", ex);
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
                    currentNestingLevel ++;
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

    private boolean isYaml(OpenApiViolationAggregator aggregator) {
        return aggregator.getOpenApiFile().getName().endsWith("yaml") || aggregator.getOpenApiFile().getName().endsWith("yml");
    }

    //TODO: Refactor to get oasVersion from parserResult, when OpenApiViolationAggregator gets refactored
    private int getSrcVersion(OpenApiViolationAggregator aggregator) {
        String mainFile = openApiFile.getName();
        List<String> src = aggregator.getSrc().get(mainFile);
        if (src.toString().contains("openapi:")) {
            return 3;
        } else {
            return 2;
        }
    }

}
