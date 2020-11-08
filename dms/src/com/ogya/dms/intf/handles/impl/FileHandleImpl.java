package com.ogya.dms.intf.handles.impl;

import java.nio.file.Path;

import com.ogya.dms.intf.handles.FileHandle;

public class FileHandleImpl implements FileHandle {

	private final Integer fileCode;
	private final Path path;
	private final Long contactId;
	private final Long groupId;

	public FileHandleImpl(Integer fileCode, Path path, Long contactId, Long groupId) {

		this.fileCode = fileCode;
		this.path = path;
		this.contactId = contactId;
		this.groupId = groupId;

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
	public Long getContactId() {

		return contactId;

	}

	@Override
	public Long getGroupId() {

		return groupId;

	}

}
