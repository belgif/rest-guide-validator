package be.belgium.gcloud.rest.styleguide.validation.core.model;

import org.eclipse.microprofile.openapi.models.media.MediaType;

public class MediaTypeDefinition extends OpenApiDefinition<MediaType> {

    /**
     * Constructor for an inline definition in response object
     */
    public MediaTypeDefinition(MediaType openApiObject, ResponseDefinition parent, String mediaType) {
        super(openApiObject, parent, mediaType, "/content/" + mediaType);
    }

    /**
     * Constructor for an inline definition in request object
     */
    public MediaTypeDefinition(MediaType openApiObject, RequestBodyDefinition parent, String mediaType) {
        super(openApiObject, parent, mediaType, "/content/" + mediaType);
    }

    @Override
    public MediaType getModel() { //override needed because of Drools' lack of generics support
        return super.getModel();
    }
}
