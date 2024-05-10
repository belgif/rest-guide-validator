package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;

import java.io.File;

public class RequestBodyDefinition extends OpenApiDefinition<RequestBody> {

    /**
     * Constructor for an inline definition in an operation
     *
     * @param openApiObject
     * @param parent
     */
    public RequestBodyDefinition(RequestBody openApiObject, OperationDefinition parent) {
        super(openApiObject, parent, "requestBody", JsonPointer.relative("requestBody"));
    }

    /**
     * Constructor for a definition under components
     *
     */
    public RequestBodyDefinition(RequestBody openApiObject, String name, File openApiFile, Parser.ParserResult result) {
        super(openApiObject, name, openApiFile, new JsonPointer("/components/requestBodies/").add(name), result);
    }

    @Override
    public RequestBody getModel() {
        return super.getModel();
    }
}
