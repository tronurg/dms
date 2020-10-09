package com.ogya.dms.intf.handles.impl;

import com.ogya.dms.intf.handles.ContactHandle;

public class ContactHandleImpl implements ContactHandle {

	private final String uuid;
	private final String name;
	private final String comment;
	private final Double lattitude;
	private final Double longitude;

	public ContactHandleImpl(String uuid, String name, String comment, Double lattitude, Double longitude) {

		this.uuid = uuid;
		this.name = name;
		this.comment = comment;
		this.lattitude = lattitude;
		this.longitude = longitude;

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

}
