package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.servers.Server;

import java.io.File;

public class ServerDefinition extends OpenApiDefinition<Server> {
    public ServerDefinition(Server model, String identifier, File openApiFile, int index, Parser.ParserResult result) {
        super(model, identifier, openApiFile, new JsonPointer("/servers/").add(index), result);
    }

    @Override
    public Server getModel() {
        return super.getModel();
    }
}
