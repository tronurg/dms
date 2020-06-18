package com.ogya.dms.server.main;

import com.ogya.dms.server.control.Control;

public class DmsServer {

	public static void main(String[] args) {

		Control.getInstance().start();

	}

}
