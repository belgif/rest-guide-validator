package be.belgium.gcloud.rest.styleguide.validation.maven.junit;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class Error {
    @XmlAttribute
    private String type;
    @XmlAttribute
    private String message;
    @XmlValue
    private String content;
}
