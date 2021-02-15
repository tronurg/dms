package com.ogya.dms.core.intf.handles.impl;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.core.intf.handles.ListHandle;

public class ListHandleImpl implements ListHandle {

	@JsonProperty("a")
	private final Integer listCode;
	@JsonProperty("b")
	private final byte[] payload;

	public ListHandleImpl(Integer listCode, byte[] payload) {

		this.listCode = listCode;
		this.payload = payload;

	}

	@Override
	public Integer getListCode() {

		return listCode;

	}

	@Override
	public <T> List<T> getList(Class<T> elementType) {

		try {

			// TODO: Conversion

			return DmsPackingFactory.unpackList(payload, elementType);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

}
