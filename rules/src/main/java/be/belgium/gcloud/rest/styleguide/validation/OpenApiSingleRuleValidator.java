package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.io.ResourceFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.List;

public class OpenApiSingleRuleValidator {

    private KieServices kieServices = KieServices.Factory.get();
    private KieContainer kieContainer;
    private StatelessKieSession kieSession;

    public OpenApiViolationAggregator isOasValid(@NotNull File file, List<String> excludedPaths) {
        return RuleRunner.execute(file, excludedPaths, kieSession);
    }

    public OpenApiSingleRuleValidator(@NotNull String ruleFile) {
        var  kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(ResourceFactory.newFileResource(ruleFile).setResourceType(ResourceType.DRL));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kieBuilder.getResults().toString());
        }

        KieRepository kieRepository = kieServices.getRepository();
        kieContainer = kieServices.newKieContainer(kieRepository.getDefaultReleaseId());
        kieSession = kieContainer.newStatelessKieSession();
    }
}
