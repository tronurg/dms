package com.ogya.dms.core.structures;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.core.common.CommonMethods;

public class GroupMessageStatus {

	@SerializedName("a")
	public final MessageStatus messageStatus;
	@SerializedName("b")
	public final List<Long> refIds;

	public GroupMessageStatus(MessageStatus messageStatus, List<Long> refIds) {

		this.messageStatus = messageStatus;
		this.refIds = refIds;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static GroupMessageStatus fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, GroupMessageStatus.class);
	}

}
