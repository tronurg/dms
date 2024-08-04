package com.dms.core.structures;

import java.nio.file.Path;

public class AttachmentPojo {

	public final Path path;
	public final Long position;

	public AttachmentPojo(Path path, Long position) {
		this.path = path;
		this.position = position;
	}

}
