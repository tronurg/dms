package com.ogya.dms.intf.handles.impl;

import com.ogya.dms.intf.handles.MessageHandle;

public class MessageHandleImpl implements MessageHandle {

	private final Integer messageCode;
	private final String message;
	private final String senderUuid;
	private final String groupUuid;

	public MessageHandleImpl(Integer messageCode, String message, String senderUuid, String groupUuid) {

		this.messageCode = messageCode;
		this.message = message;
		this.senderUuid = senderUuid;
		this.groupUuid = groupUuid;

	}

	@Override
	public Integer getMessageCode() {

		return messageCode;

	}

	@Override
	public String getMessage() {

		return message;

	}

	@Override
	public String getSenderUuid() {

		return senderUuid;

	}

	@Override
	public String getGroupUuid() {

		return groupUuid;

	}

}
