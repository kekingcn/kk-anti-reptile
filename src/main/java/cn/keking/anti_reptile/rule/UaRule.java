package cn.keking.anti_reptile.rule;

import cn.keking.anti_reptile.config.AntiReptileProperties;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author chenjh
 * @since 2019/7/17 10:13
 */
public class UaRule extends AbstractRule {

    @Autowired
    private AntiReptileProperties properties;

    @Override
    protected boolean doExecute(HttpServletRequest request, HttpServletResponse response) {
        AntiReptileProperties.UaRule uaRule = properties.getUaRule();
        UserAgent userAgent = UserAgent.parseUserAgentString(request.getHeader("User-Agent"));
        OperatingSystem os = userAgent.getOperatingSystem();
        OperatingSystem osGroup = userAgent.getOperatingSystem().getGroup();
        DeviceType deviceType = userAgent.getOperatingSystem().getDeviceType();
        if (DeviceType.UNKNOWN.equals(deviceType)) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Unknown device, User-Agent: " + userAgent.toString());
            return true;
        } else if (OperatingSystem.UNKNOWN.equals(os)
                || OperatingSystem.UNKNOWN_MOBILE.equals(os)
                || OperatingSystem.UNKNOWN_TABLET.equals(os)) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Unknown OperatingSystem, User-Agent: " + userAgent.toString());
            return true;
        }
        if (!uaRule.isAllowedLinux() && (OperatingSystem.LINUX.equals(osGroup) || OperatingSystem.LINUX.equals(os))) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Not Allowed Linux request, User-Agent: " + userAgent.toString());
            return true;
        }
        if (!uaRule.isAllowedMobile() && (DeviceType.MOBILE.equals(deviceType) || DeviceType.TABLET.equals(deviceType))) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Not Allowed Mobile Device request, User-Agent: " + userAgent.toString());
            return true;
        }
        if (!uaRule.isAllowedPc() && DeviceType.COMPUTER.equals(deviceType)) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Not Allowed PC request, User-Agent: " + userAgent.toString());
            return true;
        }
        if (!uaRule.isAllowedIot() && (DeviceType.DMR.equals(deviceType) || DeviceType.GAME_CONSOLE.equals(deviceType) || DeviceType.WEARABLE.equals(deviceType))) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Not Allowed Iot Device request, User-Agent: " + userAgent.toString());
            return true;
        }
        if (!uaRule.isAllowedProxy() && OperatingSystem.PROXY.equals(os)) {
            System.out.println("Intercepted request, uri: " + request.getRequestURI() + " Not Allowed Proxy request, User-Agent: " + userAgent.toString());
            return true;
        }
        return false;
    }

    @Override
    public void reset(HttpServletRequest request, String realRequestUri) {
        return;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
