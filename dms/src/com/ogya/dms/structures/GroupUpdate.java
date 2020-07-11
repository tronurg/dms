package com.ogya.dms.structures;

import java.util.HashMap;
import java.util.Map;

import com.ogya.dms.common.CommonMethods;

public class GroupUpdate {

	public final String groupUuid;
	public final boolean isActive;
	public String groupName;
	public final Map<String, String> contactUuidNameToBeAdded = new HashMap<String, String>();
	public final Map<String, String> contactUuidNameToBeRemoved = new HashMap<String, String>();

	public GroupUpdate(String groupUuid, boolean isActive) {

		this.groupUuid = groupUuid;
		this.isActive = isActive;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static GroupUpdate fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, GroupUpdate.class);
	}

}
