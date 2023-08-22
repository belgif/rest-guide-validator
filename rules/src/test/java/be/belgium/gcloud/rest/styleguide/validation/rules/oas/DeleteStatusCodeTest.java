package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

@Getter
public class DeleteStatusCodeTest extends AbstractOasRuleTest {
    protected String ruleName = "deleteStatusCode201";

    @ParameterizedTest
    @ValueSource(ints = {201, 202, 304, 413})
    void tests(int statusCode) throws IOException {
        ruleName = "deleteStatusCode"+statusCode;
        super.isInvalidTest();
    }

}
