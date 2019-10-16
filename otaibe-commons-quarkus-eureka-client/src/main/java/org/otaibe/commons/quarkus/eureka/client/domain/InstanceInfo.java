package org.otaibe.commons.quarkus.eureka.client.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.text.MessageFormat;

/**
 * The class that holds information required for registration with
 * <tt>Eureka Server</tt> and to be discovered by other components.
 * <p>
 * </p>
 *
 */
@Getter
@Setter
@Builder
@ToString
public class InstanceInfo {

    public static final String INSTANCE_XML_TEMPLATE = "<instance>\n" +
            "      <instanceId>{0}:{1}:{2,number,#}</instanceId>\n" +
            "      <hostName>{3}</hostName>\n" +
            "      <app>{1}</app>\n" +
            "      <ipAddr>{6}</ipAddr>\n" +
            "      <status>UP</status>\n" +
//            "      <overriddenstatus>UNKNOWN</overriddenstatus>\n" +
            "      <port enabled=\"true\">{2,number,#}</port>\n" +
            "      <securePort enabled=\"false\">443</securePort>\n" +
            "      <countryId>1</countryId>\n" +
            "      <dataCenterInfo class=\"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo\">\n" +
            "        <name>MyOwn</name>\n" +
            "      </dataCenterInfo>\n" +
//            "      <leaseInfo>\n" +
//            "        <renewalIntervalInSecs>30</renewalIntervalInSecs>\n" +
//            "        <durationInSecs>90</durationInSecs>\n" +
//            "        <registrationTimestamp>1568379719447</registrationTimestamp>\n" +
//            "        <lastRenewalTimestamp>1569684157706</lastRenewalTimestamp>\n" +
//            "        <evictionTimestamp>0</evictionTimestamp>\n" +
//            "        <serviceUpTimestamp>1568379719447</serviceUpTimestamp>\n" +
//            "      </leaseInfo>\n" +
            "      <metadata>\n" +
//            "        <management.port>9555</management.port>\n" +
            "      </metadata>\n" +
            "      <homePageUrl>http://{3}:{2,number,#}/</homePageUrl>\n" +
            "      <statusPageUrl>http://{3}:{2,number,#}{4}/info</statusPageUrl>\n" +
            "      <healthCheckUrl>http://{3}:{2,number,#}{4}/health</healthCheckUrl>\n" +
            "      <vipAddress>{1}</vipAddress>\n" +
            "      <secureVipAddress>{1}</secureVipAddress>\n" +
//            "      <isCoordinatingDiscoveryServer>false</isCoordinatingDiscoveryServer>\n" +
            "      <lastUpdatedTimestamp>{5,number,#}</lastUpdatedTimestamp>\n" +
            "      <lastDirtyTimestamp>{5,number,#}</lastDirtyTimestamp>\n" +
//            "      <actionType>ADDED</actionType>\n" +
            "    </instance>";


    String localHostName;
    String app;
    Integer port;
    String eurekaHostName;
    String ipAddress;
    String contextPath;


    public String toXmlString() {
        return MessageFormat.format(InstanceInfo.INSTANCE_XML_TEMPLATE,
                getLocalHostName(), getApp(), getPort(),
                getEurekaHostName(),
                getContextPath(), System.currentTimeMillis(),
                getIpAddress()
        );
    }

}
