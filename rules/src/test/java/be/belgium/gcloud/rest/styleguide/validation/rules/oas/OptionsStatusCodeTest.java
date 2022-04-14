package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

@Getter
public class OptionsStatusCodeTest extends AbstractOasRuleTest {
    protected String ruleName = "optionsStatusCode201";

    @ParameterizedTest
    @ValueSource(ints = {201, 204, 303, 304, 405, 409, 412, 413})
    void tests(int statusCode) throws IOException {
        ruleName = "optionsStatusCode"+statusCode;
        super.isInvalidTest();
    }
}
