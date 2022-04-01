package be.belgium.gcloud.rest.styleguide.validation.core;

public enum OperationEnum {
     GET("get"),
     POST("post"),
     PUT("put"),
     DELETE("delete"),
     PATCH("patch"),
     HEAD("head"),
     OPTIONS("options");

    public final String label;

    private OperationEnum(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
