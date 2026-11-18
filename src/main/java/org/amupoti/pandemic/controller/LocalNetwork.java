package org.amupoti.pandemic.controller;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Addresses of this computer in the local network, to open the app from a phone on the same wifi
 */
final class LocalNetwork {

    private LocalNetwork() {
    }

    static List<String> urls(int port) {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .filter(LocalNetwork::isUp)
                    .flatMap(networkInterface -> Collections.list(networkInterface.getInetAddresses()).stream())
                    .filter(address -> address instanceof Inet4Address)
                    .filter(InetAddress::isSiteLocalAddress)
                    .map(address -> "http://" + address.getHostAddress() + ":" + port + "/")
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (SocketException e) {
            return Collections.emptyList();
        }
    }

    private static boolean isUp(NetworkInterface networkInterface) {
        try {
            return networkInterface.isUp() && !networkInterface.isLoopback() && !networkInterface.isVirtual();
        } catch (SocketException e) {
            return false;
        }
    }
}
