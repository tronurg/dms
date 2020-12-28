package com.ogya.dms.core.intf.handles.impl;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.intf.handles.ObjectHandle;

public class ObjectHandleImpl implements ObjectHandle {

	private final Integer objectCode;
	private final String objectStr;
	private final Long contactId;
	private final Long groupId;

	public ObjectHandleImpl(Integer objectCode, String objectStr, Long contactId, Long groupId) {

		this.objectCode = objectCode;
		this.objectStr = objectStr;
		this.contactId = contactId;
		this.groupId = groupId;

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
	public Long getContactId() {

		return contactId;

	}

	@Override
	public Long getGroupId() {

		return groupId;

	}

}
