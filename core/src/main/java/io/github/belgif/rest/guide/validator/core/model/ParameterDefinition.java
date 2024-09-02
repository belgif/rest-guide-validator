package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import java.io.File;

public class ParameterDefinition extends OpenApiDefinition<Parameter> {
    /**
     * Constructor for an inline definition
     *
     */
    public ParameterDefinition(Parameter openApiObject, OpenApiDefinition<?> parent, String name, int index) {
        super(openApiObject, parent, name, JsonPointer.relative("parameters").add(index));
    }

    public ParameterDefinition(Parameter openApiObject, String name, File openApiFile, Parser.ParserResult result) {
        super(openApiObject, name, openApiFile, new JsonPointer("/components/parameters").add(name), result);
    }

    @Override
    public Parameter getModel() {
        return super.getModel();
    }
}
