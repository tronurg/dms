package com.ogya.dms.commons.structures;

import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AttachmentPojo {

	@JsonProperty("a")
	public long size;
	@JsonIgnore
	public Path path;

	public AttachmentPojo() {
		super();
	}

	public AttachmentPojo(Path path) {
		this.path = path;
	}

}
