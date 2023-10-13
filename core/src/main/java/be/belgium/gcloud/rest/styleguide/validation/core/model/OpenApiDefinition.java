package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.Line;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.Constructible;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
public abstract class OpenApiDefinition<T extends Constructible> {
    private T model;
    protected final DefinitionType definitionType;

    private OpenApiDefinition<?> parent; //when definitionType is INLINE

    private final String identifier; // mandatory when definitionType is TOP_LEVEL, optional otherwise

    private final File openApiFile;
    private String jsonPointer; // relative to parent object. Can be used to calculate line number

    /**
     * Constructor for an inline definition
     */
    protected OpenApiDefinition(T model, OpenApiDefinition<?> parent, String identifier, String relativeJsonPointer) {
        this.model = model;
        this.definitionType = DefinitionType.INLINE;
        this.parent = parent;
        this.identifier = identifier;
        this.openApiFile = parent.getOpenApiFile();
        this.jsonPointer = parent.getJsonPointer() + relativeJsonPointer;
    }

    /**
     * Constructor for a definition under components
     */
    protected OpenApiDefinition(T model, String identifier, File openApiFile, String jsonPointer) {
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
        // ideally we could simply search line number using the json pointer, but can't find a way

        Line lineNumber;

        //approximate algorithm, can be improved in subclasses
        if (definitionType == DefinitionType.INLINE) {
            lineNumber = parent.getLineNumber(aggregator);
            if (identifier != null) {
                lineNumber = aggregator.getLineNumber(lineNumber, identifier);
            }
        } else {
            lineNumber = getTopLevelLineNumber(aggregator);
        }

        return lineNumber;
    }

    private Line getTopLevelLineNumber(OpenApiViolationAggregator aggregator) {
        if (definitionType == DefinitionType.TOP_LEVEL) {
            List<String> pointers = Arrays.stream(jsonPointer.split("/")).filter(pointer -> !pointer.isEmpty()).collect(Collectors.toList());
            if (pointers.isEmpty()) {
                log.warn("Invalid location for definition: " + jsonPointer);
                return new Line(openApiFile.getName(), 0);
            }
            if (getSrcVersion(aggregator) == 2 && "components".equals(pointers.get(0))) {
                pointers.set(0, "definitions");
            }
            for (String fileName : aggregator.getSrc().keySet()) {
                Line line = searchObjectInFile(fileName, aggregator, pointers);
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

    private Line searchObjectInFile(String fileName, OpenApiViolationAggregator aggregator, List<String> pointers) {
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
            while (!jsonParser.isClosed()) {
                if (jsonParser.nextToken() == JsonToken.FIELD_NAME) {
                    if (pointers.get(0).equals(jsonParser.getCurrentName())) {
                        pointers.remove(0);
                        if (pointers.isEmpty()) {
                            return new Line(fileName, jsonParser.getCurrentLocation().getLineNr());
                        }
                    }
                }
            }
        } catch (IOException ex) {
            log.error("Could not parse " + fileName + " for linenumber calculation");
            throw new RuntimeException("Could not parse " + fileName + " for linenumber calculation");
        }
        return null;
    }

    private boolean isYaml(OpenApiViolationAggregator aggregator) {
        return aggregator.getOpenApiFile().getName().endsWith("yaml") || aggregator.getOpenApiFile().getName().endsWith("yml");
    }

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
