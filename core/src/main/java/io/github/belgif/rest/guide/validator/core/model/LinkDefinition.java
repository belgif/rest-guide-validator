package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.links.Link;

import java.io.File;

public class LinkDefinition extends OpenApiDefinition<Link> {

    // Constructor for inline LinkDefinition (for instance in response object)
    public LinkDefinition(Link model, OpenApiDefinition<?> parent, String identifier) {
        super(model, parent, identifier, JsonPointer.relative("links").add(identifier));
    }

    // Constructor for toplevel LinkDefinition
    public LinkDefinition(Link model, String identifier, File openApiFile, Parser.ParserResult result) {
        super(model, identifier, openApiFile, new JsonPointer("/components/links").add(identifier), result);
    }

    @Override
    public Link getModel() {
        return super.getModel();
    }
}
