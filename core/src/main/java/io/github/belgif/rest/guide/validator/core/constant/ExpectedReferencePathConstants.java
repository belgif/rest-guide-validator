package io.github.belgif.rest.guide.validator.core.constant;


import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import java.util.Map;

public class ExpectedReferencePathConstants {

    private ExpectedReferencePathConstants() {
    }

    public static final Map<Class<?>, String> OAS_2_LOCATIONS = Map.of(
            Parameter.class, "/parameters",
            RequestBody.class, "/parameters",
            APIResponse.class, "/responses",
            // OpenApi Parser sets refs to definitions in oas2 to components/schemas as well
            Schema.class, "/components/schemas",
            PathItem.class, "/paths",
            Example.class, "/examples",
            SecurityScheme.class, "/securityDefinitions"
    );

    public static final Map<Class<?>, String> OAS_3_LOCATIONS = Map.of(
            Parameter.class, "/components/parameters",
            RequestBody.class, "/components/requestBodies",
            APIResponse.class, "/components/responses",
            // OpenApi Parser sets refs to definitions in oas2 to components/schemas as well
            Schema.class, "/components/schemas",
            PathItem.class, "/paths",
            Header.class, "/components/headers",
            Example.class, "/components/examples",
            Callback.class, "/components/callbacks",
            Link.class, "/components/links",
            SecurityScheme.class, "/components/securitySchemes"
    );
}
