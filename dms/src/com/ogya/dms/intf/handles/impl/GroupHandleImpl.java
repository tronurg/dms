package com.ogya.dms.intf.handles.impl;

import java.util.List;

import com.ogya.dms.intf.handles.GroupHandle;

public class GroupHandleImpl implements GroupHandle {

	private final Long groupId;
	private final String name;
	private final String comment;
	private final List<Long> contactIds;

	public GroupHandleImpl(Long groupId, String name, String comment, List<Long> contactIds) {

		this.groupId = groupId;
		this.name = name;
		this.comment = comment;
		this.contactIds = contactIds;

	}

	@Override
	public Long getGroupId() {

		return groupId;

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
	public List<Long> getContactIds() {

		return contactIds;

	}

}
