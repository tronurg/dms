package com.ogya.dms.common;

public class CommonConstants {

	public static final String SERVER_IP = CommonMethods.getServerIp();
	public static final int SERVER_PORT = CommonMethods.getServerPort();
	public static final String DB_PATH = CommonMethods.getDbPath();
	public static final int BEACON_TIMEOUT_MS = CommonMethods.getBeaconTimeoutMs();

	public static final Integer SPECIAL_MESSAGE_CODE_TEXT = 0;
	public static final Integer SPECIAL_MESSAGE_CODE_OBJECT = 1;
	public static final Integer SPECIAL_MESSAGE_CODE_FILE = 2;
	public static final Integer SPECIAL_MESSAGE_CODE_LIST = 3;
	public static final Integer SPECIAL_MESSAGE_CODE_AUDIO = 4;
	public static final Integer SPECIAL_MESSAGE_CODE_REPORT = 5;

}
