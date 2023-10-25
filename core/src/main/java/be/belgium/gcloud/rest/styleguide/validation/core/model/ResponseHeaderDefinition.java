package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import org.eclipse.microprofile.openapi.models.headers.Header;

import java.io.File;

public class ResponseHeaderDefinition extends OpenApiDefinition<Header> {

    public ResponseHeaderDefinition(Header model, OpenApiDefinition<?> parent, String identifier) {
        super(model, parent, identifier, JsonPointer.relative("headers").add(identifier));
    }

    /**
     * Constructor for a definition under components
     */
    public ResponseHeaderDefinition(Header model, String identifier, File openApiFile) {
        super(model, identifier, openApiFile, new JsonPointer("/components/headers/").add(identifier));
    }
}
