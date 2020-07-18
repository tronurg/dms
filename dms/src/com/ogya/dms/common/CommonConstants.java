package com.ogya.dms.common;

public class CommonConstants {

	public static final String SERVER_IP = CommonMethods.getServerIp();
	public static final int SERVER_PORT = CommonMethods.getServerPort();
	public static final String DB_PATH = CommonMethods.getDbPath();
	public static final int BEACON_INTERVAL_MS = CommonMethods.getBeaconIntervalMs();
	public static final String FILE_EXPLORER_PATH = CommonMethods.getFileExplorerPath();
	public static final long MAX_FILE_LENGHT = CommonMethods.getMaxFileLenght();
	public static final String SEND_FOLDER = CommonMethods.getSendFolder();
	public static final String RECEIVE_FOLDER = CommonMethods.getReceiveFolder();

	public static final Integer CODE_UPDATE_GROUP = 0;
	public static final Integer CODE_CANCEL_MESSAGE = 1;

}
