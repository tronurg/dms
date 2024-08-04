package com.dms.server.main;

import com.dms.server.control.Control;

public class DmsServer {

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public static void main(String[] args) {

		Control.getInstance().start();

	}

}
