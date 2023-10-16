package be.belgium.gcloud.rest.styleguide.validation.core.model;

import org.eclipse.microprofile.openapi.models.PathItem;

import java.io.File;

public class PathDefinition extends OpenApiDefinition<PathItem>{

    public PathDefinition(PathItem model, String identifier, File openApiFile) {
        super(model, identifier, openApiFile, "/paths/"+escapeSlashesOnPaths(identifier));
    }

    @Override
    public PathItem getModel() {
        return super.getModel();
    }

    private static String escapeSlashesOnPaths(String jsonPointer) {
        // ~1 is used to represent / character in jsonPointer
        return jsonPointer.replaceAll("/", "~1");
    }

}
