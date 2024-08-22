package com.dms.core.intf.tools.impl;

import java.util.Objects;

import com.dms.core.database.tables.Dgroup;
import com.dms.core.intf.tools.ContactId;
import com.dms.core.intf.tools.GroupId;

public class GroupIdImpl implements GroupId {

	private final ContactId ownerId;
	private final Long refId;

	private GroupIdImpl(ContactId ownerId, Long refId) {
		super();
		this.ownerId = ownerId;
		this.refId = refId;
	}

	public static GroupId of(Dgroup group) {
		if (group == null) {
			return null;
		}
		return new GroupIdImpl(ContactIdImpl.of(group.getOwner()), group.getGroupRefId());
	}

	@Override
	public ContactId getOwnerId() {
		return ownerId;
	}

	@Override
	public Long getRefId() {
		return refId;
	}

	@Override
	public String toString() {
		return String.format("%s:%d", ownerId.toString(), refId);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GroupIdImpl)) {
			return false;
		}
		GroupIdImpl groupId = (GroupIdImpl) obj;
		return Objects.equals(this.ownerId, groupId.ownerId) && Objects.equals(this.refId, groupId.refId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ownerId, refId);
	}

}
