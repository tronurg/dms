package com.dms.core.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dms.core.control.DmsControl;
import com.dms.core.intf.DmsException;
import com.dms.core.intf.DmsHandle;
import com.dms.core.util.Commons;

public class DmsCore {

	private static final Map<String, DmsControl> INSTANCES = Collections
			.synchronizedMap(new HashMap<String, DmsControl>());

	public static void setServerIp(String SERVER_IP) {
		Commons.SERVER_IP = SERVER_IP;
	}

	public static void setServerPort(Integer SERVER_PORT) {
		Commons.SERVER_PORT = SERVER_PORT;
	}

	public static void setDbPath(String DB_PATH) {
		Commons.DB_PATH = DB_PATH;
	}

	public static void setFileExplorerPath(String FILE_EXPLORER_PATH) {
		Commons.FILE_EXPLORER_PATH = FILE_EXPLORER_PATH;
	}

	public static void setMaxFileLength(Long MAX_FILE_LENGTH) {
		Commons.MAX_FILE_LENGTH = MAX_FILE_LENGTH;
	}

	public static void setSmallFileLimit(Long SMALL_FILE_LIMIT) {
		Commons.SMALL_FILE_LIMIT = SMALL_FILE_LIMIT;
	}

	public static void setSendFolder(String SEND_FOLDER) {
		Commons.SEND_FOLDER = SEND_FOLDER;
	}

	public static void setReceiveFolder(String RECEIVE_FOLDER) {
		Commons.RECEIVE_FOLDER = RECEIVE_FOLDER;
	}

	public static void setAutoOpenFile(Boolean AUTO_OPEN_FILE) {
		Commons.AUTO_OPEN_FILE = AUTO_OPEN_FILE;
	}

	public static void addReportTemplate(String reportTemplatePath) {
		Commons.addReportTemplate(reportTemplatePath);
	}

	public static DmsHandle login(String username, String password) throws DmsException {

		try {
			checkConfiguration();
		} catch (Exception e) {
			throw new DmsException(e);
		}

		DmsControl dmsControl = INSTANCES.get(username);
		if (dmsControl == null) {
			try {
				dmsControl = new DmsControl(username, password, () -> INSTANCES.remove(username));
				INSTANCES.put(username, dmsControl);
			} catch (Exception e) {
				throw new DmsException(e);
			}
		}
		return dmsControl;

	}

	private static void checkConfiguration() throws Exception {
		List<String> confErrorList = new ArrayList<String>();
		if (Commons.SERVER_IP == null) {
			confErrorList.add("SERVER_IP");
		}
		if (Commons.SERVER_PORT == null) {
			confErrorList.add("SERVER_PORT");
		}
		if (Commons.DB_PATH == null) {
			confErrorList.add("DB_PATH");
		}
		if (Commons.FILE_EXPLORER_PATH == null) {
			confErrorList.add("FILE_EXPLORER_PATH");
		}
		if (Commons.MAX_FILE_LENGTH == null) {
			confErrorList.add("MAX_FILE_LENGTH");
		}
		if (Commons.SMALL_FILE_LIMIT == null) {
			confErrorList.add("SMALL_FILE_LIMIT");
		}
		if (Commons.SEND_FOLDER == null) {
			confErrorList.add("SEND_FOLDER");
		}
		if (Commons.RECEIVE_FOLDER == null) {
			confErrorList.add("RECEIVE_FOLDER");
		}
		if (Commons.AUTO_OPEN_FILE == null) {
			confErrorList.add("AUTO_OPEN_FILE");
		}
		if (confErrorList.isEmpty()) {
			return;
		}
		throw new Exception("Missing configuration: " + confErrorList.toString());
	}

}
