package io.github.belgif.rest.guide.validator.core.model;

import org.eclipse.microprofile.openapi.models.PathItem;

/**
 * Only contains pathItems that are in the main openapi file. Not the ones referenced.
 */
public class PathDefinition extends PathItemDefinition {

    public PathDefinition(PathItem model, PathsDefinition parent, String identifier) {
        super(model, parent, identifier);
    }

    @Override
    public PathItem getModel() {
        return super.getModel();
    }

}
