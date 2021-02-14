package com.ogya.dms.core.intf.handles.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ogya.dms.commons.structures.DmsPackingFactory;
import com.ogya.dms.core.intf.handles.ObjectHandle;

public class ObjectHandleImpl implements ObjectHandle {

	@JsonProperty("a")
	private final Integer objectCode;
	@JsonProperty("b")
	private final byte[] payload;

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
