package com.ogya.dms.core.intf.handles.impl;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.intf.handles.ObjectHandle;

public class ObjectHandleImpl implements ObjectHandle {

	@SerializedName("a")
	private final Integer objectCode;
	@SerializedName("b")
	private final String objectStr;

	public ObjectHandleImpl(Integer objectCode, String objectStr) {

		this.objectCode = objectCode;
		this.objectStr = objectStr;

	}

	@Override
	public Integer getObjectCode() {

		return objectCode;

	}

	@Override
	public <T> T getObject(Class<T> objectClass) {

		try {

			return CommonMethods.fromJson(objectStr, objectClass);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

}
