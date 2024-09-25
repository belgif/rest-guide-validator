package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
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

    private final StatelessKieSession kieSession;

    public ViolationReport callRule(@NotNull File file, List<String> excludedFiles) {
        return RuleRunner.execute(file, excludedFiles, kieSession);
    }

    public OpenApiSingleRuleValidator(@NotNull String ruleFile) {
        KieServices kieServices = KieServices.Factory.get();
        var  kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(ResourceFactory.newFileResource(ruleFile).setResourceType(ResourceType.DRL));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kieBuilder.getResults().toString());
        }

        KieRepository kieRepository = kieServices.getRepository();
        KieContainer kieContainer = kieServices.newKieContainer(kieRepository.getDefaultReleaseId());
        kieSession = kieContainer.newStatelessKieSession();
    }
}
