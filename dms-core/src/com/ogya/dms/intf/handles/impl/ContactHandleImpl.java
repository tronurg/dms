package com.ogya.dms.intf.handles.impl;

import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.intf.handles.ContactHandle;
import com.ogya.dms.structures.Availability;

public class ContactHandleImpl implements ContactHandle {

	private final Long id;
	private final String uuid;
	private final String name;
	private final String comment;
	private final Double lattitude;
	private final Double longitude;
	private final Availability availability;

	public ContactHandleImpl(Contact contact) {

		this.id = contact.getId();
		this.uuid = contact.getUuid();
		this.name = contact.getName();
		this.comment = contact.getComment();
		this.lattitude = contact.getLattitude();
		this.longitude = contact.getLongitude();
		this.availability = contact.getStatus();

	}

	@Override
	public Long getId() {

		return id;

	}

	@Override
	public String getUuid() {

		return uuid;

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
	public Double getLattitude() {

		return lattitude;

	}

	@Override
	public Double getLongitude() {

		return longitude;

	}

	@Override
	public Availability getAvailability() {

		return availability;

	}

}
