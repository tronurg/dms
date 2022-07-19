package com.ogya.dms.commons.structures;

public class StatusInfo {

	public int messageNumber;
	public int progress;

	public StatusInfo() {
		super();
	}

	public StatusInfo(int messageNumber, int progress) {
		this.messageNumber = messageNumber;
		this.progress = progress;
	}

}
