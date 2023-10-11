package be.belgium.gcloud.rest.styleguide.validation.core.model;

import org.eclipse.microprofile.openapi.models.headers.Header;

import java.io.File;

public class ResponseHeaderDefinition extends OpenApiDefinition<Header> {

    public ResponseHeaderDefinition(Header model, OpenApiDefinition<?> parent, String identifier, String relativeJsonPointer) {
        super(model, parent, identifier, relativeJsonPointer);
    }

    public ResponseHeaderDefinition(Header model, OpenApiDefinition<?> parent, String identifier) {
        super(model, parent, identifier, "/headers/" + identifier);
    }

    /**
     * Constructor for a definition under components
     *
     */
    public ResponseHeaderDefinition(Header model, String identifier, File openApiFile) {
        super(model, identifier, openApiFile, "/components/headers/" + identifier);
    }
}
