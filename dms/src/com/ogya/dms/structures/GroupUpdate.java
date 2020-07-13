package com.ogya.dms.structures;

import java.util.Map;

import com.ogya.dms.common.CommonMethods;

public class GroupUpdate {

	public final String uuid;
	public String name;
	public Boolean active;
	public Map<String, String> add;
	public Map<String, String> remove;

	public GroupUpdate(String uuid) {

		this.uuid = uuid;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static GroupUpdate fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, GroupUpdate.class);
	}

}
