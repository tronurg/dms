package com.dms.server.main;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.dms.server.control.Control;
import com.dms.server.util.Commons;

public class DmsServer {

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public static void setIntercomPort(int INTERCOM_PORT) {
		Commons.INTERCOM_PORT = INTERCOM_PORT;
	}

	public static void setMulticastGroup(String MULTICAST_GROUP) {
		Commons.MULTICAST_GROUP = MULTICAST_GROUP;
	}

	public static void setBeaconPort(int BEACON_PORT) {
		Commons.BEACON_PORT = BEACON_PORT;
	}

	public static void setServerPort(int SERVER_PORT) {
		Commons.SERVER_PORT = SERVER_PORT;
	}

	public static void setClientPortFrom(int CLIENT_PORT_FROM) {
		Commons.CLIENT_PORT_FROM = CLIENT_PORT_FROM;
	}

	public static void setClientPortTo(int CLIENT_PORT_TO) {
		Commons.CLIENT_PORT_TO = CLIENT_PORT_TO;
	}

	public static void setBeaconIntervalMs(int BEACON_INTERVAL_MS) {
		Commons.BEACON_INTERVAL_MS = BEACON_INTERVAL_MS;
	}

	public static void setCertificateFolder(String CERTIFICATE_FOLDER) {
		Commons.CERTIFICATE_FOLDER = CERTIFICATE_FOLDER;
	}

	public static void setIpDatFolder(String IP_DAT_FOLDER) {
		Commons.IP_DAT_FOLDER = IP_DAT_FOLDER;
	}

	public static void addPreferredIp(InetAddress preferredIp) {
		Commons.PREFERRED_IPS.add(preferredIp);
	}

	public static void start() throws Exception {
		checkConfiguration();
		Control.getInstance().start();
	}

	private static void checkConfiguration() throws Exception {
		List<String> confErrorList = new ArrayList<String>();
		if (Commons.INTERCOM_PORT == null) {
			confErrorList.add("INTERCOM_PORT");
		}
		if (Commons.MULTICAST_GROUP == null) {
			confErrorList.add("MULTICAST_GROUP");
		}
		if (Commons.BEACON_PORT == null) {
			confErrorList.add("BEACON_PORT");
		}
		if (Commons.SERVER_PORT == null) {
			confErrorList.add("SERVER_PORT");
		}
		if (Commons.CLIENT_PORT_FROM == null) {
			confErrorList.add("CLIENT_PORT_FROM");
		}
		if (Commons.CLIENT_PORT_TO == null) {
			confErrorList.add("CLIENT_PORT_TO");
		}
		if (Commons.BEACON_INTERVAL_MS == null) {
			confErrorList.add("BEACON_INTERVAL_MS");
		}
		if (Commons.CERTIFICATE_FOLDER == null) {
			confErrorList.add("CERTIFICATE_FOLDER");
		}
		if (confErrorList.isEmpty()) {
			return;
		}
		throw new Exception("Missing configuration: " + confErrorList.toString());
	}

}
