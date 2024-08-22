package com.dms.core.intf.handles.impl;

import java.util.ArrayList;
import java.util.List;

import com.dms.core.database.tables.Dgroup;
import com.dms.core.intf.handles.GroupHandle;
import com.dms.core.intf.tools.ContactId;
import com.dms.core.intf.tools.GroupId;
import com.dms.core.intf.tools.impl.ContactIdImpl;
import com.dms.core.intf.tools.impl.GroupIdImpl;
import com.dms.core.structures.Availability;

public class GroupHandleImpl implements GroupHandle {

	private final GroupId groupId;
	private final String name;
	private final String comment;
	private final List<ContactId> contactIds = new ArrayList<ContactId>();
	private final Availability availability;

	public GroupHandleImpl(Dgroup group) {
		this.groupId = GroupIdImpl.of(group);
		this.name = group.getName();
		this.comment = group.getComment();
		group.getMembers().forEach(contact -> contactIds.add(ContactIdImpl.of(contact)));
		this.availability = group.getStatus();
	}

	@Override
	public GroupId getGroupId() {
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
	public List<ContactId> getContactIds() {
		return contactIds;
	}

	@Override
	public Availability getAvailability() {
		return availability;
	}

}
