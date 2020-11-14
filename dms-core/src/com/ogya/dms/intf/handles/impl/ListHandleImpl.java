package com.ogya.dms.intf.handles.impl;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.intf.handles.ListHandle;

public class ListHandleImpl implements ListHandle {

	private final Integer listCode;
	private final String listStr;
	private final Long contactId;
	private final Long groupId;

	public ListHandleImpl(Integer listCode, String listStr, Long contactId, Long groupId) {

		this.listCode = listCode;
		this.listStr = listStr;
		this.contactId = contactId;
		this.groupId = groupId;

	}

	@Override
	public Integer getListCode() {

		return listCode;

	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> getList(Class<T> elementType) {

		try {

			return Arrays.asList((T[]) CommonMethods.fromJson(
					CommonMethods.convertListJsonFromCommon(listStr, elementType.getSimpleName()),
					Array.newInstance(elementType, 0).getClass()));

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
