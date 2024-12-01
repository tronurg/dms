package com.dms.server.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Commons {

	public static final String DMS_UUID = UUID.randomUUID().toString();

	public static Integer INTERCOM_PORT;
	public static String MULTICAST_GROUP;
	public static Integer BEACON_PORT;
	public static Integer SERVER_PORT;
	public static Integer CLIENT_PORT_FROM;
	public static Integer CLIENT_PORT_TO;
	public static Integer BEACON_INTERVAL_MS;
	public static String CERTIFICATE_FOLDER;
	public static String IP_DAT_FOLDER = "./";
	public static final List<InetAddress> PREFERRED_IPS = new ArrayList<InetAddress>();

	public static final int CONN_TIMEOUT_MS = 5000;

	public static List<InetAddress> getLocalIPv4Addresses() {

		List<InetAddress> addrs = new ArrayList<InetAddress>();

		try {
			for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (InetAddress ia : Collections.list(ni.getInetAddresses())) {
					if (isAddressAllowed(ia)) {
						addrs.add(ia);
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		return addrs;

	}

	public static boolean isAddressAllowed(InetAddress addr) {
		if (!(addr instanceof Inet4Address)) {
			return false;
		}
		return !(addr.isAnyLocalAddress() || addr.isLinkLocalAddress() || addr.isLoopbackAddress()
				|| addr.isMulticastAddress());
	}

	public static int getLocalAddressPriority(InetAddress localAddress) {

		int priority = PREFERRED_IPS.indexOf(localAddress);

		if (priority < 0) {
			priority = PREFERRED_IPS.size();
		}

		return priority;

	}

}
