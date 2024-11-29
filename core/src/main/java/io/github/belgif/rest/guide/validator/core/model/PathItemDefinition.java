package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import org.eclipse.microprofile.openapi.models.PathItem;

/**
 * Contains all pathItems.
 */
public class PathItemDefinition extends OpenApiDefinition<PathItem>{

    public PathItemDefinition(PathItem model, PathsDefinition parent, String identifier) {
        super(model, parent, identifier, JsonPointer.relative(identifier));
    }

    @Override
    public PathItem getModel() {
        return super.getModel();
    }

}
