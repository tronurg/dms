package com.ogya.dms.core.structures;

import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DownloadPojo {

	public final transient String senderUuid;
	@JsonProperty("a")
	public final Integer fileId;
	@JsonProperty("b")
	public final Long downloadId;
	@JsonProperty("c")
	public Long position;
	public transient Path path;

	public DownloadPojo(String senderUuid, Integer fileId, Long downloadId) {
		this.senderUuid = senderUuid;
		this.fileId = fileId;
		this.downloadId = downloadId;
	}

}
