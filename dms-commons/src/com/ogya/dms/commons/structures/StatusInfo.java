package com.ogya.dms.commons.structures;

public class StatusInfo {

	public String address;
	public int messageNumber;
	public int progress;

	public StatusInfo() {
		super();
	}

	public StatusInfo(String address, int messageNumber, int progress) {
		this.address = address;
		this.messageNumber = messageNumber;
		this.progress = progress;
	}

}
