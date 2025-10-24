package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.Getter;
import org.eclipse.microprofile.openapi.models.Paths;

import java.io.File;

@Getter
public class PathsDefinition extends OpenApiDefinition<Paths> {

    private final boolean inMainFile;

    public PathsDefinition(Paths model, File openApiFile, Parser.ParserResult result) {
        super(model, "paths", openApiFile, new JsonPointer("/paths"), result);
        this.inMainFile = openApiFile.equals(result.getOpenApiFile());
    }

    @Override
    public Paths getModel() {
        return super.getModel();
    }

}
