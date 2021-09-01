package com.ogya.dms.server.common;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public class CommonConstants {

	public static final String IO_TMP_DIR = System.getProperty("java.io.tmpdir");
	public static final String DMS_UUID = UUID.randomUUID().toString();

	public static final int INTERCOM_PORT = CommonMethods.getIntercomPort();
	public static final String MULTICAST_IP = CommonMethods.getMulticastIp();
	public static final int MULTICAST_PORT = CommonMethods.getMulticastPort();
	public static final int SERVER_PORT = CommonMethods.getServerPort();
	public static final int CLIENT_PORT_FROM = CommonMethods.getClientPortFrom();
	public static final int CLIENT_PORT_TO = CommonMethods.getClientPortTo();
	public static final int BEACON_INTERVAL_MS = CommonMethods.getBeaconIntervalMs();
	public static final long SMALL_FILE_LIMIT = CommonMethods.getSmallFileLimit();
	public static final String SHARED_TMP_DIR = CommonMethods.getSharedTmpDir();
	public static final List<InetAddress> PREFERRED_IPS = CommonMethods.getPreferredIps();

	public static final int CONN_TIMEOUT_MS = 5000;

}
