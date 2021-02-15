package com.ogya.dms.core.intf.handles.impl;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.intf.handles.ListHandle;

public class ListHandleImpl implements ListHandle {

	@JsonProperty("a")
	private final Integer listCode;
	@JsonProperty("b")
	private final String payload;

	public ListHandleImpl(Integer listCode, String payload) {

		this.listCode = listCode;
		this.payload = payload;

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
					CommonMethods.convertListJsonFromCommon(payload, elementType.getSimpleName()),
					Array.newInstance(elementType, 0).getClass()));

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

}
