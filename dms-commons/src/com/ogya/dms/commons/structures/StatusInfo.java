package com.ogya.dms.commons.structures;

public class StatusInfo {

	public String address;
	public boolean success;

	public StatusInfo() {
		super();
	}

	public StatusInfo(String address, boolean success) {
		this.address = address;
		this.success = success;
	}

}
