package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;

import java.io.File;

public class SecurityRequirementDefinition extends OpenApiDefinition<SecurityRequirement> {

    // Constructor for inline securityRequirement (in operations)
    public SecurityRequirementDefinition(SecurityRequirement model, OpenApiDefinition<?> parent, int index) {
        super(model, parent, null, JsonPointer.relative("security").add(index));
    }

    // Constructor for toplevel securityRequirement
    public SecurityRequirementDefinition(SecurityRequirement model, int index, File openApiFile, Parser.ParserResult result) {
        super(model, null, openApiFile, new JsonPointer("/security").add(index), result);
    }

    @Override
    public SecurityRequirement getModel() {
        return super.getModel();
    }
}
