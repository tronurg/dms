package com.dms.core.structures;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GroupUpdate {

	@JsonProperty("a")
	public String name;
	@JsonProperty("b")
	public Boolean active;
	@JsonProperty("c")
	public List<ContactMap> add;
	@JsonProperty("d")
	public List<Long> remove;

	public GroupUpdate() {
		super();
	}

}
