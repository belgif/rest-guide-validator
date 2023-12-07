package be.belgium.gcloud.rest.styleguide.validation.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Violation implements Comparable<Violation> {
    String ruleName;
    String message;
    public ViolationType type;
    Line lineNumber;

    @Override
    public String toString() {
        return (lineNumber.getLineNumber() > 0 ? String.format("file: %15s: ln%4d:", lineNumber.getFileName(), lineNumber.getLineNumber()) : "") +
                (lineNumber.getLineNumber() == 0 && type.equals(ViolationType.IGNORED) ? String.format("file: %23s:", lineNumber.getFileName()) : "") +
                String.format("  %-14S ", ("[" + type + "]")) +
                String.format("%-17s ", ruleName) +
                message;
    }

    @Override
    public int compareTo(Violation o) {
        if (lineNumber.getFileName().equals(o.lineNumber.getFileName())) {
            return Integer.compare(lineNumber.getLineNumber(), o.lineNumber.getLineNumber());
        } else {
            return lineNumber.getFileName().compareTo(o.lineNumber.getFileName());
        }
    }
}
