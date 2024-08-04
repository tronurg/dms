package com.dms.core.intf.tools.impl;

import com.dms.core.intf.tools.GroupId;

public class GroupIdImpl implements GroupId {

	private final Long value;

	private GroupIdImpl(Long value) {
		super();
		this.value = value;
	}

	public static GroupId of(Long value) {
		if (value == null) {
			return null;
		}
		return new GroupIdImpl(value);
	}

	@Override
	public Long getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GroupIdImpl)) {
			return false;
		}
		GroupIdImpl groupId = (GroupIdImpl) obj;
		return this.value.equals(groupId.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

}
