package com.ogya.dms.core.structures;

import com.google.gson.annotations.SerializedName;

public class ContactMap {

	@SerializedName("a")
	public Long refId;
	@SerializedName("b")
	public String uuid;
	@SerializedName("c")
	public String name;

	public ContactMap(Long refId, String uuid, String name) {
		this.refId = refId;
		this.uuid = uuid;
		this.name = name;
	}

}
