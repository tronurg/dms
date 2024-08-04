package com.dms.core.intf.handles.impl;

import java.nio.file.Path;

import com.dms.core.intf.handles.FileHandle;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FileHandleImpl implements FileHandle {

	@JsonProperty("a")
	private Integer fileCode;
	private transient Path path;
	@JsonProperty("b")
	private String fileName;

	public FileHandleImpl() {
		super();
	}

	public FileHandleImpl(Integer fileCode, Path path) {

		this.fileCode = fileCode;
		this.path = path;
		this.fileName = path.getFileName().toString();

	}

	public String getFileName() {

		return fileName;

	}

	@Override
	public Integer getFileCode() {

		return fileCode;

	}

	@Override
	public Path getPath() {

		return path;

	}

}
