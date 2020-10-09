package com.ogya.dms.intf.handles.impl;

import java.nio.file.Path;

import com.ogya.dms.intf.handles.FileHandle;

public class FileHandleImpl implements FileHandle {

	private final Integer fileCode;
	private final Path path;
	private final String senderUuid;
	private final String groupUuid;

	public FileHandleImpl(Integer fileCode, Path path, String senderUuid, String groupUuid) {

		this.fileCode = fileCode;
		this.path = path;
		this.senderUuid = senderUuid;
		this.groupUuid = groupUuid;

	}

	@Override
	public Integer getFileCode() {

		return fileCode;

	}

	@Override
	public Path getPath() {

		return path;

	}

	@Override
	public String getSenderUuid() {

		return senderUuid;

	}

	@Override
	public String getGroupUuid() {

		return groupUuid;

	}

}
