package com.dms.core.structures;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DownloadPojo {

	public transient String senderUuid;
	@JsonProperty("a")
	public Long fileId;
	@JsonProperty("b")
	public Long downloadId;
	@JsonProperty("c")
	public Long position;
	public transient Path path;
	public final transient AtomicBoolean pausing = new AtomicBoolean();
	public final transient AtomicBoolean paused = new AtomicBoolean();

	public DownloadPojo() {
		super();
	}

	public DownloadPojo(String senderUuid, Long fileId, Long downloadId) {
		this.senderUuid = senderUuid;
		this.fileId = fileId;
		this.downloadId = downloadId;
	}

}
