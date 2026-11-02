package com.nutriworks.util;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Utility for local area network (LAN) IPv4 resolution and dynamic port scanning.
 * Accurately filters out loopback, link-local, Docker, Hyper-V, WSL, VirtualBox,
 * VMware, and VPN adapters to determine the laptop's real Wi-Fi or Ethernet IP.
 */
public final class NetworkUtils {

    private NetworkUtils() {
    }

    /**
     * Resolves the primary LAN IPv4 address of the host machine.
     *
     * @param overrideIp Optional manual IP override from user settings.
     * @return Best candidate IPv4 string, or "127.0.0.1" if offline.
     */
    public static String getBestLanIp(String overrideIp) {
        if (overrideIp != null && !overrideIp.trim().isEmpty()) {
            return overrideIp.trim();
        }

        try {
            // First attempt: Connect a datagram socket to a public IP (does not send data)
            // This queries the operating system routing table for the primary outgoing adapter
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                InetAddress localAddr = socket.getLocalAddress();
                if (localAddr instanceof Inet4Address && !localAddr.isLoopbackAddress() && !localAddr.isLinkLocalAddress()) {
                    return localAddr.getHostAddress();
                }
            }
        } catch (Exception ignored) {
            // Fall through to network interface iteration
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                for (NetworkInterface ni : Collections.list(interfaces)) {
                    if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) {
                        continue;
                    }

                    String name = (ni.getName() + " " + ni.getDisplayName()).toLowerCase();
                    if (name.contains("docker") || name.contains("veth") || name.contains("virtual") ||
                            name.contains("vmnet") || name.contains("hyper-v") || name.contains("wsl") ||
                            name.contains("tailscale") || name.contains("zerotier") || name.contains("vpn") ||
                            name.contains("tap") || name.contains("tun")) {
                        continue;
                    }

                    for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ignored) {
        }

        return "127.0.0.1";
    }

    /**
     * Checks if a local port is available for binding, or finds the next available port.
     *
     * @param preferredPort The default port (e.g. 8080).
     * @return An available port number.
     */
    public static int findAvailablePort(int preferredPort) {
        if (isPortAvailable(preferredPort)) {
            return preferredPort;
        }

        for (int port = preferredPort + 1; port <= preferredPort + 50; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }

        // Fallback to random ephemeral port
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return preferredPort;
        }
    }

    public static boolean isPortAvailable(int port) {
        try (ServerSocket ss = new ServerSocket(port)) {
            ss.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
