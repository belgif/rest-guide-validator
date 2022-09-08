package be.belgium.gcloud.rest.styleguide.validation.rules.oas2;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

@Getter
public class DefaultMediaTypeTest extends AbstractOasRuleTest {
    protected String ruleName = "defaultMediaTypeProduce";

    @ParameterizedTest
    @ValueSource(strings = {"defaultMediaTypeProduce", "defaultMediaTypeConsume"})
    void testGetDefaultMediaTypeProduce(String ruleName) throws IOException {
        this.ruleName = ruleName;
        super.isInvalidTest();
    }
}
