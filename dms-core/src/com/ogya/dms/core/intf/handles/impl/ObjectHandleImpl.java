package com.ogya.dms.core.intf.handles.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.intf.handles.ObjectHandle;

public class ObjectHandleImpl implements ObjectHandle {

	@JsonProperty("a")
	private final Integer objectCode;
	@JsonProperty("b")
	private final String payload;

	public ObjectHandleImpl(Integer objectCode, String payload) {

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

			return CommonMethods.fromJson(payload, objectClass);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

}
