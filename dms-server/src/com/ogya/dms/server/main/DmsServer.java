package com.ogya.dms.server.main;

import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.control.Control;

public class DmsServer {

	public static void main(String[] args) {

		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.io.tmpdir", CommonConstants.TEMP_DIR);

		Control.getInstance().start();

	}

}
