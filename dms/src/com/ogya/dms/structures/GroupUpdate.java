package com.ogya.dms.structures;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonSyntaxException;
import com.ogya.dms.common.CommonMethods;

public class GroupUpdate {

	public final String groupUuid;
	public String groupName;
	public final Map<String, String> contactUuidNameToBeAdded = new HashMap<String, String>();
	public final Map<String, String> contactUuidNameToBeRemoved = new HashMap<String, String>();

	public GroupUpdate(String groupUuid) {

		this.groupUuid = groupUuid;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static GroupUpdate fromJson(String json) throws JsonSyntaxException {
		return CommonMethods.fromJson(json, GroupUpdate.class);
	}

}
