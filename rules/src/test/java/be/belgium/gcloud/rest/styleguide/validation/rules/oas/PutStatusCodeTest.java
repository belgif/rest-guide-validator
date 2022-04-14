package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

@Getter
public class PutStatusCodeTest extends AbstractOasRuleTest {
    protected String ruleName = "putStatusCode202";

    @ParameterizedTest
    @ValueSource(ints = {202, 303, 304})
    void tests(int statusCode) throws IOException {
        ruleName = "putStatusCode"+statusCode;
        super.isInvalidTest();
    }
}
