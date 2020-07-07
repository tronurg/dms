package com.ogya.dms.structures;

import com.google.gson.JsonSyntaxException;
import com.ogya.dms.common.CommonMethods;

public class FilePojo {

	public final String fileName;
	public final String fileContent;

	public FilePojo(String fileName, String fileContent) {

		this.fileName = fileName;
		this.fileContent = fileContent;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static FilePojo fromJson(String json) throws JsonSyntaxException {
		return CommonMethods.fromJson(json, FilePojo.class);
	}

}
