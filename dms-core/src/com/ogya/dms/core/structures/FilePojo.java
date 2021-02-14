package com.ogya.dms.core.structures;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FilePojo {

	@JsonProperty("a")
	public final String fileName;
	@JsonProperty("b")
	public final byte[] payload;

	public FilePojo(String fileName, byte[] payload) {

		this.fileName = fileName;
		this.payload = payload;

	}

}
