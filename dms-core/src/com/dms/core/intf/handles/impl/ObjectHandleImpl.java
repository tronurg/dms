package com.dms.core.intf.handles.impl;

import com.dms.commons.DmsPackingFactory;
import com.dms.core.intf.handles.ObjectHandle;
import com.fasterxml.jackson.annotation.JsonProperty;

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
