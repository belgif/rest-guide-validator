package be.belgium.gcloud.rest.styleguide.validation.core;

import be.belgium.gcloud.rest.styleguide.validation.core.model.SchemaDefinition;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserTest {

    @Test
    public void testConstructNestedSchema() {
        var oas = new OpenApiViolationAggregator();
        var openApiFile = new File(getClass().getResource("../rules/schemasOpenApi.yaml").getFile());
        var parserResult = new Parser(openApiFile).parse(oas);
        Optional<SchemaDefinition> jsonPointer = parserResult.getSchemas().stream().filter(def -> def.getJsonPointer().endsWith("anyOf/1")).findAny();
        assertTrue(jsonPointer.isPresent());
    }

}
