package com.ogya.dms.structures;

import com.ogya.dms.common.CommonMethods;

public class FilePojo {

	public final String fileName;
	public final String fileContent;
	public final int originalLength;

	public FilePojo(String fileName, String fileContent, int originalLength) {

		this.fileName = fileName;
		this.fileContent = fileContent;
		this.originalLength = originalLength;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static FilePojo fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, FilePojo.class);
	}

}
