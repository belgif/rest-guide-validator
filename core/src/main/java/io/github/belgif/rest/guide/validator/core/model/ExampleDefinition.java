package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.Getter;
import org.eclipse.microprofile.openapi.models.examples.Example;

import java.io.File;

public class ExampleDefinition extends OpenApiDefinition<Example> {

    @Getter
    private final boolean isOasExampleObject;

    // Constructor for inline ExampleDefinition with Example object as modelinput, used when parent has a map with Examples.
    public ExampleDefinition(Example model, OpenApiDefinition<?> parent, String name) {
        super(model, parent, name, JsonPointer.relative("examples").add(name));
        this.isOasExampleObject = true;
    }

    // Constructor for inline ExampleDefinition, used when there is a single example in the parent object.
    public ExampleDefinition(Example model, OpenApiDefinition<?> parent) {
        super(model, parent, null, JsonPointer.relative("example"));
        this.isOasExampleObject = false;
    }

    // Constructor for toplevel ExampleDefinition.
    public ExampleDefinition(Example model, String name, File openApiFile, Parser.ParserResult result) {
        super(model, name, openApiFile, new JsonPointer("/components/examples").add(name), result);
        this.isOasExampleObject = true;
    }

    @Override
    public Example getModel() {
        return super.getModel();
    }
}
