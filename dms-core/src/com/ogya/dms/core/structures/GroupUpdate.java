package com.ogya.dms.core.structures;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.core.common.CommonMethods;

public class GroupUpdate {

	@SerializedName("a")
	public String name;
	@SerializedName("b")
	public Boolean active;
	@SerializedName("c")
	public List<ContactMap> add;
	@SerializedName("d")
	public List<Long> remove;

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static GroupUpdate fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, GroupUpdate.class);
	}

}
