package com.ogya.dms.common;

public class CommonConstants {

	public static final String SERVER_IP = CommonMethods.getServerIp();
	public static final int SERVER_PORT = CommonMethods.getServerPort();
	public static final String DB_PATH = CommonMethods.getDbPath();
	public static final int BEACON_INTERVAL_MS = CommonMethods.getBeaconIntervalMs();

	public static final Integer CODE_INSTANT_TEXT = 0;
	public static final Integer CODE_INSTANT_FILE = 1;

	public static final Integer CODE_UPDATE_GROUP = 100;
	public static final Integer CODE_UPDATE_MESSAGE = 101;

}
