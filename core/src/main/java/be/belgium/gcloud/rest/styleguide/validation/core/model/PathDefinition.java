package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import org.eclipse.microprofile.openapi.models.PathItem;

import java.io.File;

public class PathDefinition extends OpenApiDefinition<PathItem>{

    public PathDefinition(PathItem model, String identifier, File openApiFile) {
        super(model, identifier, openApiFile, new JsonPointer("/paths").add(identifier));
    }

    @Override
    public PathItem getModel() {
        return super.getModel();
    }

}
