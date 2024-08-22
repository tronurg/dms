package com.dms.core.intf.tools.impl;

import java.util.Objects;

import com.dms.core.database.tables.Contact;
import com.dms.core.intf.tools.ContactId;

public class ContactIdImpl implements ContactId {

	private final String uuid;

	private ContactIdImpl(String uuid) {
		super();
		this.uuid = uuid;
	}

	public static ContactId of(Contact contact) {
		if (contact == null) {
			return null;
		}
		return new ContactIdImpl(contact.getUuid());
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public String toString() {
		return uuid.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ContactIdImpl)) {
			return false;
		}
		ContactIdImpl contactId = (ContactIdImpl) obj;
		return Objects.equals(this.uuid, contactId.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

}
