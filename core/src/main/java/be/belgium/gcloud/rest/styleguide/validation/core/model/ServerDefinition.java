package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
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
