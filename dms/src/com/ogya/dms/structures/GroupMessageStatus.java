package com.ogya.dms.structures;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.common.CommonMethods;

public class GroupMessageStatus {

	@SerializedName(value = "a")
	public final MessageStatus messageStatus;
	@SerializedName(value = "b")
	public Long[] ids;

	public GroupMessageStatus(MessageStatus messageStatus) {

		this.messageStatus = messageStatus;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static GroupMessageStatus fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, GroupMessageStatus.class);
	}

}
