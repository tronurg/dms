package com.dms.core.structures;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GroupMessageStatus {

	@JsonProperty("a")
	public MessageStatus messageStatus;
	@JsonProperty("b")
	public List<Long> refIds;

	public GroupMessageStatus() {
		super();
	}

	public GroupMessageStatus(MessageStatus messageStatus, List<Long> refIds) {

		this.messageStatus = messageStatus;
		this.refIds = refIds;

	}

}
