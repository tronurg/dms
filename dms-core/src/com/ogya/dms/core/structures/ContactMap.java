package com.ogya.dms.core.structures;

import com.google.gson.annotations.SerializedName;

public class ContactMap {

	@SerializedName(value = "a")
	public Long refId;
	@SerializedName(value = "b")
	public String uuid;
	@SerializedName(value = "c")
	public String name;

	public ContactMap(Long refId, String uuid, String name) {
		this.refId = refId;
		this.uuid = uuid;
		this.name = name;
	}

}
