package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.openapitools.empoa.swagger.core.internal.models.examples.SwExample;

import java.io.File;

public class ExampleDefinition extends OpenApiDefinition<Example> {

    // Constructor for inline ExampleDefinition with Example object as modelinput, used when parent has a map with Examples.
    public ExampleDefinition(Example model, OpenApiDefinition<?> parent, String name) {
        super(model, parent, name, JsonPointer.relative("examples").add(name));
    }

    // Constructor for inline ExampleDefinition, used when there is a single example in the parent object.
    public ExampleDefinition(Example model, OpenApiDefinition<?> parent) {
        super(model, parent, null, JsonPointer.relative("example"));
    }

    // Constructor for toplevel ExampleDefinition.
    public ExampleDefinition(Example model, String name, File openApiFile, Parser.ParserResult result) {
        super(model, name, openApiFile, new JsonPointer("/components/examples").add(name), result);
    }

    @Override
    public Example getModel() {
        return super.getModel();
    }
}
