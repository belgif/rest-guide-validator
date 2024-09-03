package io.github.belgif.rest.guide.validator.core.model.helper;

import lombok.Getter;

@Getter
public class MediaType {

    private final String type;
    private final String subType;
    private final boolean typeWildcard;
    private final boolean subTypeWildcard;

    public MediaType(String mediaType) {
        if (mediaType == null || !mediaType.contains("/")) {
            throw new MediaTypeException("Invalid media type: " + mediaType);
        }
        mediaType = mediaType.contains(";") ? mediaType.substring(0, mediaType.indexOf(";")) : mediaType;
        String[] parts = mediaType.split("/");
        if (parts.length != 2) {
            throw new MediaTypeException("Invalid media type: " + mediaType);
        }
        this.type = parts[0].toLowerCase();
        this.subType = parts[1].toLowerCase();
        typeWildcard = "*".equals(this.type);
        subTypeWildcard = "*".equals(this.subType);
    }

    public String getSubTypeSuffix() {
        int suffixIndex = this.subType.lastIndexOf('+');
        if (suffixIndex != -1 && this.subType.length()-1 > suffixIndex) {
            return this.subType.substring(suffixIndex + 1);
        }
        return null;
    }

    public boolean includes(MediaType mediaType) {
        return this.typeCompatible(mediaType) && this.subTypeCompatible(mediaType);
    }

    private boolean typeCompatible(MediaType mediaType) {
        return this.type.equals(mediaType.getType()) || this.typeWildcard;
    }

    private boolean subTypeCompatible(MediaType mediaType) {
        return this.subType.equals(mediaType.getSubType()) ||
                this.subTypeWildcard ||
                this.subType.equals(mediaType.getSubTypeSuffix());
    }

    @Override
    public String toString() {
        return this.type + "/" + this.subType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MediaType mediaType = (MediaType) o;
        return type.equals(mediaType.type) && subType.equals(mediaType.subType);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + subType.hashCode();
        return result;
    }
}
