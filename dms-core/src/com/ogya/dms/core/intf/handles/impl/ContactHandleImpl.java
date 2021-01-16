package com.ogya.dms.core.intf.handles.impl;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.structures.Availability;

public class ContactHandleImpl implements ContactHandle {

	private final Long id;
	private final String uuid;
	private final String name;
	private final String comment;
	private final Double lattitude;
	private final Double longitude;
	private final Availability availability;
	private final List<InetAddress> remoteInterfaces = new ArrayList<InetAddress>();
	private final List<InetAddress> localInterfaces = new ArrayList<InetAddress>();

	public ContactHandleImpl(Contact contact) {

		this.id = contact.getId();
		this.uuid = contact.getUuid();
		this.name = contact.getName();
		this.comment = contact.getComment();
		this.lattitude = contact.getLattitude();
		this.longitude = contact.getLongitude();
		this.availability = contact.getStatus();
		this.remoteInterfaces.addAll(contact.getRemoteInterfaces());
		this.localInterfaces.addAll(contact.getLocalInterfaces());

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

	@Override
	public List<InetAddress> getRemoteInterfaces() {

		return remoteInterfaces;

	}

	@Override
	public List<InetAddress> getLocalInterfaces() {

		return localInterfaces;

	}

}
