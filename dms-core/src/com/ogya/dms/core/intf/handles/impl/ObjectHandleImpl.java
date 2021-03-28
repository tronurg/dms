package com.ogya.dms.core.intf.handles.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.core.intf.handles.ObjectHandle;

public class ObjectHandleImpl implements ObjectHandle {

	@JsonProperty("a")
	private Integer objectCode;
	@JsonProperty("b")
	private byte[] payload;

	public ObjectHandleImpl() {
		super();
	}

	public ObjectHandleImpl(Integer objectCode, byte[] payload) {

		this.objectCode = objectCode;
		this.payload = payload;

	}

	@Override
	public Integer getObjectCode() {

		return objectCode;

	}

	@Override
	public <T> T getObject(Class<T> objectClass) {

		try {

			return DmsPackingFactory.unpack(payload, objectClass);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

}
