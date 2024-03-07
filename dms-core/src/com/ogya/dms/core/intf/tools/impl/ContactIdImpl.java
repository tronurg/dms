package com.ogya.dms.core.intf.tools.impl;

import com.ogya.dms.core.intf.tools.ContactId;

public class ContactIdImpl implements ContactId {

	private final Long value;

	private ContactIdImpl(Long value) {
		super();
		this.value = value;
	}

	public static ContactId of(Long value) {
		if (value == null) {
			return null;
		}
		return new ContactIdImpl(value);
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
