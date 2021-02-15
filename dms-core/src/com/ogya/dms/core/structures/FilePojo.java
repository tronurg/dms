package com.ogya.dms.core.structures;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FilePojo {

	@JsonProperty("a")
	public String fileName;
	@JsonProperty("b")
	public byte[] payload;

	public FilePojo() {
		super();
	}

	public FilePojo(String fileName, byte[] payload) {

		this.fileName = fileName;
		this.payload = payload;

	}

}
