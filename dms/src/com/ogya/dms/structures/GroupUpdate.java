package com.ogya.dms.structures;

import java.util.Map;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.common.CommonMethods;

public class GroupUpdate {

	@SerializedName(value = "a")
	public String name;
	@SerializedName(value = "b")
	public Boolean active;
	@SerializedName(value = "c")
	public Map<String, String> add;
	@SerializedName(value = "d")
	public Map<String, String> remove;

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static GroupUpdate fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, GroupUpdate.class);
	}

}
