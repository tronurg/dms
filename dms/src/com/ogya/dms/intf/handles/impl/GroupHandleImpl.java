package com.ogya.dms.intf.handles.impl;

import java.util.List;

import com.ogya.dms.intf.handles.GroupHandle;

public class GroupHandleImpl implements GroupHandle {

	private final String groupUuid;
	private final String name;
	private final String comment;
	private final List<String> contactUuids;

	public GroupHandleImpl(String groupUuid, String name, String comment, List<String> contactUuids) {

		this.groupUuid = groupUuid;
		this.name = name;
		this.comment = comment;
		this.contactUuids = contactUuids;

	}

	@Override
	public String getGroupUuid() {

		return groupUuid;

	}

	@Override
	public String getName() {

		return name;

	}

	@Override
	public String getComment() {

		return comment;

	}

	@Override
	public List<String> getContactUuids() {

		return contactUuids;

	}

}
