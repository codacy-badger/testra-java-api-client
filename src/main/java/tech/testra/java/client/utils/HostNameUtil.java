package tech.testra.java.client.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostNameUtil {

    private HostNameUtil() {
    }

    public static String hostName() {
        String hostName = "UNKNOWN_HOST";
        try {
            InetAddress address = InetAddress.getLocalHost();
            hostName = address.getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return hostName;
    }
}
