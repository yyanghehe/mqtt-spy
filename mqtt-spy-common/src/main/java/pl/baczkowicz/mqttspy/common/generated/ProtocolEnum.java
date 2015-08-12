//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.08.12 at 01:35:43 PM BST 
//


package pl.baczkowicz.mqttspy.common.generated;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ProtocolEnum.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ProtocolEnum"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="MQTT Default"/&gt;
 *     &lt;enumeration value="MQTT 3.1.1"/&gt;
 *     &lt;enumeration value="MQTT 3.1"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ProtocolEnum")
@XmlEnum
public enum ProtocolEnum {

    @XmlEnumValue("MQTT Default")
    MQTT_DEFAULT("MQTT Default"),
    @XmlEnumValue("MQTT 3.1.1")
    MQTT_3_1_1("MQTT 3.1.1"),
    @XmlEnumValue("MQTT 3.1")
    MQTT_3_1("MQTT 3.1");
    private final String value;

    ProtocolEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ProtocolEnum fromValue(String v) {
        for (ProtocolEnum c: ProtocolEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
