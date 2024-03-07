package com.ogya.dms.core.intf.tools.impl;

import com.ogya.dms.core.intf.tools.GroupId;

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

}
