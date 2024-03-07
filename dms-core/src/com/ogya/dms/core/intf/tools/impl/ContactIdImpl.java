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

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ContactIdImpl)) {
			return false;
		}
		ContactIdImpl contactId = (ContactIdImpl) obj;
		return this.value.equals(contactId.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

}
