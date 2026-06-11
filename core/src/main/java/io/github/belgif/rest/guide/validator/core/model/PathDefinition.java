package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import lombok.Getter;
import org.eclipse.microprofile.openapi.models.PathItem;

@Getter
public class PathDefinition extends OpenApiDefinition<PathItem> {

    /**
     * Indicates if path is a direct (reachable) pathItem or referenced pathItem
     */
    private final boolean isDirectPath;
    private final boolean hasReusableDefinitionsOnly;

    public PathDefinition(PathItem model, PathsDefinition parent, String identifier) {
        super(model, parent, identifier, JsonPointer.relative(identifier));
        this.hasReusableDefinitionsOnly = hasReusableDefinitionsOnly();
        this.isDirectPath = parent.isInMainFile() && !hasReusableDefinitionsOnly;
    }

    @Override
    public PathItem getModel() {
        return super.getModel();
    }

    private boolean hasReusableDefinitionsOnly() {
        return this.getResult().getSrc().get(this.getOpenApiFile().getAbsolutePath()).hasReusableDefinitionsOnly();
    }
}
