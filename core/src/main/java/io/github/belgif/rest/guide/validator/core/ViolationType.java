package io.github.belgif.rest.guide.validator.core;

public enum ViolationType {
    MANDATORY("mandatory"), RECOMMENDED("recommended"), STYLE("style"), IGNORED("ignored");
    public final String value;

    ViolationType(String value) {
        this.value = value;
    }
}
