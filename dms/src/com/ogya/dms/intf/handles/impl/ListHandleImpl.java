package com.ogya.dms.intf.handles.impl;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.intf.handles.ListHandle;

public class ListHandleImpl implements ListHandle {

	private final Integer listCode;
	private final String listStr;
	private final String contactUuid;
	private final String groupUuid;

	public ListHandleImpl(Integer listCode, String listStr, String contactUuid, String groupUuid) {

		this.listCode = listCode;
		this.listStr = listStr;
		this.contactUuid = contactUuid;
		this.groupUuid = groupUuid;

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
	public String getContactUuid() {

		return contactUuid;

	}

	@Override
	public String getGroupUuid() {

		return groupUuid;

	}

}
