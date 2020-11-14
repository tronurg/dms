package com.ogya.dms.server.common;

public class CommonConstants {

	public static final int INTERCOM_PORT = CommonMethods.getIntercomPort();
	public static final String MULTICAST_IP = CommonMethods.getMulticastIp();
	public static final int MULTICAST_PORT = CommonMethods.getMulticastPort();
	public static final int BEACON_INTERVAL_MS = CommonMethods.getBeaconIntervalMs();
	public static final int SERVER_PORT = CommonMethods.getServerPort();
	public static final int CLIENT_PORT_FROM = CommonMethods.getClientPortFrom();
	public static final int CLIENT_PORT_TO = CommonMethods.getClientPortTo();

	public static final int CONN_TIMEOUT_MS = 5000;

}
