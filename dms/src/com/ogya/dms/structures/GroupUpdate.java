package com.ogya.dms.structures;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.common.CommonMethods;

public class GroupUpdate {

	@SerializedName(value = "a")
	public String name;
	@SerializedName(value = "b")
	public Boolean active;
	@SerializedName(value = "c")
	public Map<Long, String> add;
	@SerializedName(value = "d")
	public List<Long> remove;
	@SerializedName(value = "e")
	public ContactMap contactMap;

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static GroupUpdate fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, GroupUpdate.class);
	}

}
