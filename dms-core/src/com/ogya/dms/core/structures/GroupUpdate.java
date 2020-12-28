package com.ogya.dms.core.structures;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.core.common.CommonMethods;

public class GroupUpdate {

	@SerializedName(value = "a")
	public String name;
	@SerializedName(value = "b")
	public Boolean active;
	@SerializedName(value = "c")
	public List<ContactMap> add;
	@SerializedName(value = "d")
	public List<Long> remove;

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static GroupUpdate fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, GroupUpdate.class);
	}

}
