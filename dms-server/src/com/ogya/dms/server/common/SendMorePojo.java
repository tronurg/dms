package com.ogya.dms.server.common;

public class SendMorePojo {

	public final String localUser;
	public final String targetServer;

	public SendMorePojo(String localUser, String targetServer) {
		this.localUser = localUser;
		this.targetServer = targetServer;
	}

}
