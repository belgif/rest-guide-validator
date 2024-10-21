package io.github.belgif.rest.guide.validator.core;

public enum ViolationLevel {
    MANDATORY("mandatory"), RECOMMENDED("recommended"), STYLE("style"), IGNORED("ignored");
    public final String value;

    ViolationLevel(String value) {
        this.value = value;
    }
}
