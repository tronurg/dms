package com.ogya.dms.core.intf.handles.impl;

import java.nio.file.Path;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.structures.FilePojo;

public class FileHandleImpl implements FileHandle {

	@SerializedName(value = "a")
	private final Integer fileCode;
	private final Path path;
	@SerializedName(value = "b")
	private final FilePojo filePojo;

	public FileHandleImpl(Integer fileCode, Path path) {

		this.fileCode = fileCode;
		this.path = path;
		this.filePojo = null;

	}

	public FileHandleImpl(Integer fileCode, FilePojo filePojo) {

		this.fileCode = fileCode;
		this.path = null;
		this.filePojo = filePojo;

	}

	public FilePojo getFilePojo() {

		return filePojo;

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
