package com.ogya.dms.core.structures;

import java.nio.file.Path;

public class FileBuilder {

	private final String fileName;
	private final AttachmentType attachmentType;
	private final Integer fileCode;
	private final PathSupplier pathSupplier;

	public FileBuilder(String fileName, AttachmentType attachmentType, Integer fileCode, PathSupplier pathSupplier) {

		this.fileName = fileName;
		this.attachmentType = attachmentType;
		this.fileCode = fileCode;
		this.pathSupplier = pathSupplier;

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

	public Path buildAndGet() throws Exception {

		return pathSupplier.get();

	}

	public static interface PathSupplier {

		Path get() throws Exception;

	}

}
