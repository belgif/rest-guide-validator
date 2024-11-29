package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.Paths;

import java.io.File;

public class PathsDefinition extends OpenApiDefinition<Paths>{

    public PathsDefinition(Paths model, File openApiFile, Parser.ParserResult result) {
        super(model, "paths", openApiFile, new JsonPointer("/paths"), result);
    }

    @Override
    public Paths getModel() {
        return super.getModel();
    }

}
