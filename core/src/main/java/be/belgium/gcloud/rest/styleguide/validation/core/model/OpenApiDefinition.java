package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import lombok.Getter;
import org.eclipse.microprofile.openapi.models.Constructible;

import java.io.File;

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

    public int getLineNumber(OpenApiViolationAggregator aggregator) {
        // ideally we could simply search line number using the json pointer, but can't find a way

        int lineNumber;

        //approximate algorithm, can be improved in subclasses
        if(definitionType == DefinitionType.INLINE) {
            lineNumber = parent.getLineNumber(aggregator);
            if (identifier != null) {
                lineNumber = aggregator.getLineNumber(lineNumber, identifier);
            }
        } else {
            lineNumber = aggregator.getLineNumber(identifier);
        }

        return lineNumber;
    }

}
