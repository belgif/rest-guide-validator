package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import org.eclipse.microprofile.openapi.models.servers.Server;

import java.io.File;

public class ServerDefinition extends OpenApiDefinition<Server> {
    public ServerDefinition(Server model, String identifier, File openApiFile, int index) {
        super(model, identifier, openApiFile, new JsonPointer("/servers/").add(index));
    }

    @Override
    public Server getModel() {
        return super.getModel();
    }
}
