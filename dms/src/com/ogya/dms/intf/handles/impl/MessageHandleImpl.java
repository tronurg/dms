package com.ogya.dms.intf.handles.impl;

import com.ogya.dms.intf.handles.MessageHandle;

public class MessageHandleImpl implements MessageHandle {

	private final Integer messageCode;
	private final String message;
	private final Long contactId;
	private final Long groupId;

	public MessageHandleImpl(Integer messageCode, String message, Long contactId, Long groupId) {

		this.messageCode = messageCode;
		this.message = message;
		this.contactId = contactId;
		this.groupId = groupId;

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
	public Long getContactId() {

		return contactId;

	}

	@Override
	public Long getGroupId() {

		return groupId;

	}

}
