package com.ogya.dms.core.structures;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContactMap {

	@JsonProperty("a")
	public Long refId;
	@JsonProperty("b")
	public String uuid;
	@JsonProperty("c")
	public String name;

	public ContactMap(Long refId, String uuid, String name) {
		this.refId = refId;
		this.uuid = uuid;
		this.name = name;
	}

}
