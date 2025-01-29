package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import org.drools.model.codegen.ExecutableModelProject;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.io.ResourceFactory;

import jakarta.validation.constraints.NotNull;
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
        // ExecutableModelProject.class compiles DRLs to Java (so we can build without deprecated drools-mvel)
        // https://docs.drools.org/8.44.0.Final/drools-docs/drools/KIE/index.html#executable-model-modify-proc_packaging-deploying
        kieBuilder.buildAll(ExecutableModelProject.class);


        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kieBuilder.getResults().toString());
        }

        KieRepository kieRepository = kieServices.getRepository();
        KieContainer kieContainer = kieServices.newKieContainer(kieRepository.getDefaultReleaseId());
        kieSession = kieContainer.newStatelessKieSession();
    }
}
