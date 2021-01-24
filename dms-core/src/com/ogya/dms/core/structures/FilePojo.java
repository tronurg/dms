package com.ogya.dms.core.structures;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.core.common.CommonMethods;

public class FilePojo {

	@SerializedName("a")
	public final String fileName;
	@SerializedName("b")
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
