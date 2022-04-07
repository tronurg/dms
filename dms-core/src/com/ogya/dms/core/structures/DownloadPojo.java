package com.ogya.dms.core.structures;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DownloadPojo {

	public final transient String senderUuid;
	@JsonProperty("a")
	public final Long fileId;
	@JsonProperty("b")
	public final Long downloadId;
	@JsonProperty("c")
	public Long position;
	public transient Path path;
	public final transient AtomicBoolean paused = new AtomicBoolean();

	public DownloadPojo(String senderUuid, Long fileId, Long downloadId) {
		this.senderUuid = senderUuid;
		this.fileId = fileId;
		this.downloadId = downloadId;
	}

}
