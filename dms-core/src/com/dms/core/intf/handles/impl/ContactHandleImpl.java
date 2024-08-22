package com.dms.core.intf.handles.impl;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.dms.core.database.tables.Contact;
import com.dms.core.intf.handles.ContactHandle;
import com.dms.core.intf.tools.ContactId;
import com.dms.core.intf.tools.impl.ContactIdImpl;
import com.dms.core.structures.Availability;

public class ContactHandleImpl implements ContactHandle {

	private final ContactId id;
	private final String name;
	private final String comment;
	private final Double latitude;
	private final Double longitude;
	private final Availability availability;
	private final String secretId;
	private final Map<InetAddress, InetAddress> localRemoteServerIps = new HashMap<InetAddress, InetAddress>();

	public ContactHandleImpl(Contact contact) {
		this.id = ContactIdImpl.of(contact);
		this.name = contact.getName();
		this.comment = contact.getComment();
		this.latitude = contact.getLatitude();
		this.longitude = contact.getLongitude();
		this.availability = contact.getStatus();
		this.secretId = contact.getSecretId();
		this.localRemoteServerIps.putAll(contact.getLocalRemoteServerIps());
	}

	@Override
	public ContactId getId() {
		return id;
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
	public Double getLatitude() {
		return latitude;
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
