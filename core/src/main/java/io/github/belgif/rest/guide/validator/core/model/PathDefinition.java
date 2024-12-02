package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import org.eclipse.microprofile.openapi.models.PathItem;

public class PathDefinition extends OpenApiDefinition<PathItem>{

    public PathDefinition(PathItem model, PathsDefinition parent, String identifier) {
        super(model, parent, identifier, JsonPointer.relative(identifier));
    }

    @Override
    public PathItem getModel() {
        return super.getModel();
    }

}
