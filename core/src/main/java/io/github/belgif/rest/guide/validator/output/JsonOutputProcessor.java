package io.github.belgif.rest.guide.validator.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.output.model.ViolationEntry;
import io.github.belgif.rest.guide.validator.output.model.ViolationGroup;
import io.github.belgif.rest.guide.validator.output.model.ViolationReport;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonOutputProcessor extends OutputProcessor implements DirectoryOutputProcessor {
    private static final String DEFAULT_FILE_NAME = "validationReport.json";
    /**
     * Output directory
     */
    private File outputDirectory;

    private File outputFile;

    public JsonOutputProcessor(OutputGroupBy outputGroupBy, File outputFile) {
        super(outputGroupBy);
        this.outputFile = outputFile;
    }

    @Override
    public void setOutputDirectory(File outputFile) {
        this.outputDirectory = outputFile;
    }

    @Override
    public void process(OpenApiViolationAggregator violationAggregator) {
        writeToFile(mapToViolationReport(violationAggregator));
    }

    private ViolationReport mapToViolationReport(OpenApiViolationAggregator violationAggregator) {
        var violations = this.getOutputGroupBy().groupViolations(violationAggregator.getViolations());
        LinkedHashMap<String, ViolationGroup> groups = new LinkedHashMap<>();
        for (Map.Entry<String, List<Violation>> entry : violations.entrySet()) {
            groups.put(getOutputGroupBy().getIdentifier(entry.getValue().get(0)),
                    new ViolationGroup(
                            entry.getValue().size(),
                            entry.getValue().stream().map(this::mapToViolationFileObject).toList()));
        }
        return new ViolationReport(
                violationAggregator.getAmountOfActionableViolations(),
                violationAggregator.getAmountOfIgnoredViolations(),
                this.getOutputGroupBy().value,
                groups
        );
    }

    private ViolationEntry mapToViolationFileObject(Violation violation) {
        return new ViolationEntry(
                violation.getRuleId(),
                violation.getDescription(),
                violation.getMessage(),
                violation.getLevel().value,
                violation.getLineNumber().getFileName(),
                violation.getLineNumber().getLineNumber(),
                "#" + violation.getPointer());
    }

    private void writeToFile(ViolationReport violationReport) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            var file = resolveOutputFile();
            file.getParentFile().mkdirs();
            mapper.writeValue(file, violationReport);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File resolveOutputFile() {
        if (outputFile == null) {
            return new File(outputDirectory, DEFAULT_FILE_NAME).getAbsoluteFile();
        }
        return outputFile.getAbsoluteFile();
    }

}
