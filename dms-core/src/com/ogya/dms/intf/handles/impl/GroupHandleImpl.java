package com.ogya.dms.intf.handles.impl;

import java.util.ArrayList;
import java.util.List;

import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.intf.handles.GroupHandle;
import com.ogya.dms.structures.Availability;

public class GroupHandleImpl implements GroupHandle {

	private final Long groupId;
	private final Long groupRefId;
	private final Long ownerId;
	private final String name;
	private final String comment;
	private final List<Long> contactIds = new ArrayList<Long>();
	private final Availability availability;

	public GroupHandleImpl(Dgroup group) {

		this.groupId = group.getId();
		this.groupRefId = group.getGroupRefId();
		this.ownerId = group.getOwner().getId();
		this.name = group.getName();
		this.comment = group.getComment();
		group.getMembers().forEach(contact -> contactIds.add(contact.getId()));
		this.availability = group.getStatus();

	}

	@Override
	public Long getGroupId() {

		return groupId;

	}

	@Override
	public Long getGroupRefId() {

		return groupRefId;

	}

	@Override
	public Long getOwnerId() {

		return ownerId;

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

	@Override
	public Availability getAvailability() {

		return availability;

	}

}
