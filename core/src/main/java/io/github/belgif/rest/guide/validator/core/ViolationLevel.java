package io.github.belgif.rest.guide.validator.core;

public enum ViolationLevel {
    REQUIRED("required"), RECOMMENDED("recommended"), IGNORED("ignored");
    public final String value;

    ViolationLevel(String value) {
        this.value = value;
    }
}
