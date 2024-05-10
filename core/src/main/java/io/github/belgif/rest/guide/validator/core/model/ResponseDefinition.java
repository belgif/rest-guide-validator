package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.Getter;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import java.io.File;

public class ResponseDefinition extends OpenApiDefinition<APIResponse> {

    @Getter
    private final String statusCode;


    /**
     * Constructor for an inline definition
     *
     * @param openApiObject
     * @param parent
     * @param statusCode
     */
    public ResponseDefinition(APIResponse openApiObject, OperationDefinition parent, String statusCode) {
        super(openApiObject, parent, statusCode, JsonPointer.relative("responses").add(statusCode));
        this.statusCode = statusCode;
    }

    /**
     * Constructor for a definition under components
     *
     * @param openApiObject
     * @param name
     * @param openApiFile
     */
    public ResponseDefinition(APIResponse openApiObject, String name, File openApiFile, Parser.ParserResult result) {
        super(openApiObject, name, openApiFile, new JsonPointer("/components/responses/").add(name), result);
        this.statusCode = null;
    }

    @Override
    public APIResponse getModel() {
        return super.getModel();
    }
}
