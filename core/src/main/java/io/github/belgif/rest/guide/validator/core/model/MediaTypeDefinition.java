package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.media.MediaType;

public class MediaTypeDefinition extends OpenApiDefinition<MediaType> {

    /**
     * Constructor for an inline definition
     */
    public MediaTypeDefinition(MediaType openApiObject, OpenApiDefinition parent,  String mediaType){
        super(openApiObject, parent, mediaType, JsonPointer.relative("content").add(mediaType));
    }

    @Override
    public MediaType getModel() { //override needed because of Drools' lack of generics support
        return super.getModel();
    }
}
