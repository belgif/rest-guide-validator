package io.github.belgif.rest.guide.validator.runner.output.junit;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.*;

/**
 * Generated class to write a XML output file (junit)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class Failure {
    @XmlAttribute
    private String type;
    @XmlAttribute
    private String message; // Error Details in Jenkins
    @XmlValue
    private String content;

}
