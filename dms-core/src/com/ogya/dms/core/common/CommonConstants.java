package com.ogya.dms.core.common;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.ogya.dms.core.view.ReportsPane.ReportTemplate;

public class CommonConstants {

	public static final String SERVER_IP = CommonMethods.getServerIp();
	public static final int SERVER_PORT = CommonMethods.getServerPort();
	public static final String DB_PATH = CommonMethods.getDbPath();
	public static final String FILE_EXPLORER_PATH = CommonMethods.getFileExplorerPath();
	public static final long MAX_FILE_LENGTH = CommonMethods.getMaxFileLength();
	public static final String SEND_FOLDER = CommonMethods.getSendFolder();
	public static final String RECEIVE_FOLDER = CommonMethods.getReceiveFolder();
	public static final boolean AUTO_OPEN_FILE = CommonMethods.getAutoOpenFile();

	public static final List<ReportTemplate> REPORT_TEMPLATES = CommonMethods.getReportTemplates();

	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuuMMddHHmmss");

	public static final int CHUNK_SIZE = 8192;

}
