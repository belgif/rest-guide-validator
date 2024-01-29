package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import java.io.File;

public class SecuritySchemeDefinition extends OpenApiDefinition<SecurityScheme> {

    // Constructor for toplevel SecuritySchemeDefinition
    public SecuritySchemeDefinition(SecurityScheme model, String identifier, File openApiFile, Parser.ParserResult result) {
        super(model, identifier, openApiFile, new JsonPointer("/components/securitySchemes").add(identifier), result);
    }

    @Override
    public SecurityScheme getModel() {
        return super.getModel();
    }
}
