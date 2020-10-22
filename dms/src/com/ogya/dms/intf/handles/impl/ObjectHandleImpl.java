package com.ogya.dms.intf.handles.impl;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.intf.handles.ObjectHandle;

public class ObjectHandleImpl implements ObjectHandle {

	private final Integer objectCode;
	private final String objectStr;
	private final String contactUuid;
	private final String groupUuid;

	public ObjectHandleImpl(Integer objectCode, String objectStr, String contactUuid, String groupUuid) {

		this.objectCode = objectCode;
		this.objectStr = objectStr;
		this.contactUuid = contactUuid;
		this.groupUuid = groupUuid;

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

	@Override
	public String getContactUuid() {

		return contactUuid;

	}

	@Override
	public String getGroupUuid() {

		return groupUuid;

	}

}
