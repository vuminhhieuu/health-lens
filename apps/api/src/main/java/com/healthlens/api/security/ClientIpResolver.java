package com.healthlens.api.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public final class ClientIpResolver {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}"
                    + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
    );

    private static final Pattern NUMERIC_IPV6_LITERAL_PATTERN = Pattern.compile("^[0-9a-fA-F:.]+$");

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String firstIp = xForwardedFor.split(",", 2)[0].trim();
            if (isValidIpAddress(firstIp)) {
                return firstIp;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return isValidIpAddress(remoteAddr) ? remoteAddr : "unknown";
    }

    private static boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank() || "unknown".equalsIgnoreCase(ipAddress)) {
            return false;
        }
        if (IPV4_PATTERN.matcher(ipAddress).matches()) {
            return true;
        }
        if (!ipAddress.contains(":") || !NUMERIC_IPV6_LITERAL_PATTERN.matcher(ipAddress).matches()) {
            return false;
        }
        try {
            InetAddress.getByName(ipAddress);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
