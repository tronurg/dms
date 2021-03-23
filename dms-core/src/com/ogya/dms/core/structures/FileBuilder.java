package com.ogya.dms.core.structures;

import java.nio.file.Path;
import java.util.function.Supplier;

public class FileBuilder {

	private final String fileName;
	private final AttachmentType attachmentType;
	private final Integer fileCode;
	private final Supplier<Path> fileBuilder;

	public FileBuilder(String fileName, AttachmentType attachmentType, Integer fileCode, Supplier<Path> fileBuilder) {

		this.fileName = fileName;
		this.attachmentType = attachmentType;
		this.fileCode = fileCode;
		this.fileBuilder = fileBuilder;

	}

	public String getFileName() {

		return fileName;

	}

	public AttachmentType getAttachmentType() {

		return attachmentType;

	}

	public Integer getFileCode() {

		return fileCode;

	}

	public Path buildAndGet() {

		return fileBuilder.get();

	}

}
