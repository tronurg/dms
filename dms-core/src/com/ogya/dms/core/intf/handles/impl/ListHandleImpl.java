package com.ogya.dms.core.intf.handles.impl;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.intf.handles.ListHandle;

public class ListHandleImpl implements ListHandle {

	@SerializedName(value = "a")
	private final Integer listCode;
	@SerializedName(value = "b")
	private final String listStr;

	public ListHandleImpl(Integer listCode, String listStr) {

		this.listCode = listCode;
		this.listStr = listStr;

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

}
