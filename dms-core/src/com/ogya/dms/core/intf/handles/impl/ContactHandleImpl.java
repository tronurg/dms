package com.ogya.dms.core.intf.handles.impl;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

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
	private final String secretId;
	private final Map<InetAddress, InetAddress> localRemoteServerIps = new HashMap<InetAddress, InetAddress>();

	public ContactHandleImpl(Contact contact) {

		this.id = contact.getId();
		this.uuid = contact.getUuid();
		this.name = contact.getName();
		this.comment = contact.getComment();
		this.lattitude = contact.getLattitude();
		this.longitude = contact.getLongitude();
		this.availability = contact.getStatus();
		this.secretId = contact.getSecretId();
		this.localRemoteServerIps.putAll(contact.getLocalRemoteServerIps());

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
	public String getSecretId() {

		return secretId;

	}

	@Override
	public Map<InetAddress, InetAddress> getLocalRemoteServerIps() {

		return localRemoteServerIps;

	}

}
