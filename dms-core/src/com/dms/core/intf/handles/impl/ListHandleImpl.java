package com.dms.core.intf.handles.impl;

import java.util.List;

import com.dms.commons.DmsPackingFactory;
import com.dms.core.intf.handles.ListHandle;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ListHandleImpl implements ListHandle {

	@JsonProperty("a")
	private Integer listCode;
	@JsonProperty("b")
	private byte[] payload;

	public ListHandleImpl() {
		super();
	}

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

			return DmsPackingFactory.unpackList(payload, elementType);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

}
