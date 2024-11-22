package io.github.belgif.rest.guide.validator.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@EqualsAndHashCode
public class Violation implements Comparable<Violation> {
    private final String ruleId;
    // general description of the rule. Should be the same for all instances of the same ruleId
    private final String description;
    // specific message for this violation
    private String message;
    private final ViolationLevel level;
    private final Line lineNumber;
    private final String pointer;
    @Setter
    private String reportMessage = null;

    public Violation(String ruleId, String description, String message, ViolationLevel level, Line lineNumber, String pointer) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be null");
        }
        this.ruleId = ruleId;
        this.description = description;
        this.message = message;
        this.level = level;
        this.lineNumber = lineNumber;
        this.pointer = pointer;
    }

    public Violation(String ruleId, String description, ViolationLevel level, Line lineNumber, String pointer) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be null");
        }
        this.ruleId = ruleId;
        this.description = description;
        this.level = level;
        this.lineNumber = lineNumber;
        this.pointer = pointer;
    }

    @Override
    public String toString() {
        return String.format("%-14S ", ("[" + level + "]")) +
                String.format("%-14s ", ruleId) +
                (lineNumber.getLineNumber() > 0 ? String.format("%-15s ln%4d  ", lineNumber.getFileName(), lineNumber.getLineNumber()) : "") +
                (lineNumber.getLineNumber() == 0 && level.equals(ViolationLevel.IGNORED) ? String.format("%-15s ", lineNumber.getFileName()) : "") +
                String.format("%s: %n", "#" + pointer) +
                description + getFormattedMessage();
    }

    @Override
    /*
     * Will order violations based on:
     * Type (MANDATORY, RECOMMENDED, STYLE, IGNORED)
     * Filename
     * Line number
     */
    public int compareTo(Violation o) {
        if (level.equals(o.getLevel())) {
            if (lineNumber.getFileName().equals(o.lineNumber.getFileName())) {
                return Integer.compare(lineNumber.getLineNumber(), o.lineNumber.getLineNumber());
            } else {
                return lineNumber.getFileName().compareTo(o.lineNumber.getFileName());
            }
        } else {
            return level.compareTo(o.getLevel());
        }
    }

    public String getFormattedMessage() {
        if (message == null) {
            return "";
        }
        return (message.contains("\n") ? String.format("%n%s", message) : (" -- " + message)).replace("\n", "\n-- ");
    }

    public String getReportMessage() {
        return Objects.requireNonNullElseGet(reportMessage, () -> description + " " + getFormattedMessage());
    }

}
