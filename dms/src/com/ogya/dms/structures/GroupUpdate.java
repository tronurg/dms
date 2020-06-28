package com.ogya.dms.structures;

import java.util.HashMap;
import java.util.Map;

public class GroupUpdate {

	public final String groupUuid;
	public String groupName;
	public final Map<String, String> contactUuidNameToBeAdded = new HashMap<String, String>();
	public final Map<String, String> contactUuidNameToBeRemoved = new HashMap<String, String>();

	public GroupUpdate(String groupUuid) {

		this.groupUuid = groupUuid;

	}

}
