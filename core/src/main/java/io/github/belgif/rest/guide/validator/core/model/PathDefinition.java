package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.PathItem;

import java.io.File;

public class PathDefinition extends OpenApiDefinition<PathItem>{

    public PathDefinition(PathItem model, String identifier, File openApiFile, Parser.ParserResult result) {
        super(model, identifier, openApiFile, new JsonPointer("/paths").add(identifier), result);
    }

    @Override
    public PathItem getModel() {
        return super.getModel();
    }

}
