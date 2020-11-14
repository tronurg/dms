package com.ogya.dms.structures;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.common.CommonMethods;

public class FilePojo {

	@SerializedName(value = "a")
	public final String fileName;
	@SerializedName(value = "b")
	public final String fileContent;

	public FilePojo(String fileName, String fileContent) {

		this.fileName = fileName;
		this.fileContent = fileContent;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static FilePojo fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, FilePojo.class);
	}

}
