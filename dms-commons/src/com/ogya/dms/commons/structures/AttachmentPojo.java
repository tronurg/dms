package com.ogya.dms.commons.structures;

import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AttachmentPojo {

	@JsonProperty("a")
	public long size;
	@JsonProperty("b")
	public Path link;
	@JsonIgnore
	public Path source;

	public AttachmentPojo(Path path, boolean linkOnly) {
		if (linkOnly) {
			this.link = path;
		} else {
			this.source = path;
		}
	}

}
