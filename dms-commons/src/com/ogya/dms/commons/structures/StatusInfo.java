package com.ogya.dms.commons.structures;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatusInfo {

	@JsonProperty("a")
	public String address;
	@JsonProperty("b")
	public int messageNumber;
	@JsonProperty("c")
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
