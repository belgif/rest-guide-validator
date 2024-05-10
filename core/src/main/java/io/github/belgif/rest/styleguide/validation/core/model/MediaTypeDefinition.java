package io.github.belgif.rest.styleguide.validation.core.model;

import io.github.belgif.rest.styleguide.validation.core.parser.JsonPointer;
import org.eclipse.microprofile.openapi.models.media.MediaType;

public class MediaTypeDefinition extends OpenApiDefinition<MediaType> {

    /**
     * Constructor for an inline definition in response object
     */
    public MediaTypeDefinition(MediaType openApiObject, ResponseDefinition parent, String mediaType) {
        super(openApiObject, parent, mediaType, JsonPointer.relative("content").add(mediaType));
    }

    /**
     * Constructor for an inline definition in request object
     */
    public MediaTypeDefinition(MediaType openApiObject, RequestBodyDefinition parent, String mediaType) {
        super(openApiObject, parent, mediaType, JsonPointer.relative("content").add(mediaType));
    }

    /**
     * Constructor for an inline definition in responseHeader object
     */
    public MediaTypeDefinition(MediaType openApiObject, ResponseHeaderDefinition parent, String mediaType) {
        super(openApiObject, parent, mediaType, JsonPointer.relative("content").add(mediaType));
    }

    /**
     * Constructor for an inline definition in parameter object
     */
    public MediaTypeDefinition(MediaType openApiObject, ParameterDefinition parent, String mediaType) {
        super(openApiObject, parent, mediaType, JsonPointer.relative("content").add(mediaType));
    }

    @Override
    public MediaType getModel() { //override needed because of Drools' lack of generics support
        return super.getModel();
    }
}
