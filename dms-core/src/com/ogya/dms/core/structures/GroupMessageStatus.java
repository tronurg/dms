package com.ogya.dms.core.structures;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GroupMessageStatus {

	@JsonProperty("a")
	public final MessageStatus messageStatus;
	@JsonProperty("b")
	public final List<Long> refIds;

	public GroupMessageStatus(MessageStatus messageStatus, List<Long> refIds) {

		this.messageStatus = messageStatus;
		this.refIds = refIds;

	}

}
