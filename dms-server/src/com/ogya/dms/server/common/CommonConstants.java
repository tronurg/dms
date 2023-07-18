package com.ogya.dms.server.common;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public class CommonConstants {

	public static final String DMS_UUID = UUID.randomUUID().toString();

	public static final int INTERCOM_PORT = CommonMethods.getIntercomPort();
	public static final int BEACON_PORT = CommonMethods.getBeaconPort();
	public static final int SERVER_PORT = CommonMethods.getServerPort();
	public static final int CLIENT_PORT_FROM = CommonMethods.getClientPortFrom();
	public static final int CLIENT_PORT_TO = CommonMethods.getClientPortTo();
	public static final int BEACON_INTERVAL_MS = CommonMethods.getBeaconIntervalMs();
	public static final List<InetAddress> PREFERRED_IPS = CommonMethods.getPreferredIps();

	public static final int CONN_TIMEOUT_MS = 5000;

}
