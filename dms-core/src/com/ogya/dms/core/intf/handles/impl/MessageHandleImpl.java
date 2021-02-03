package com.ogya.dms.core.intf.handles.impl;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.ListHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.intf.handles.ObjectHandle;

public class MessageHandleImpl implements MessageHandle {

	@SerializedName("a")
	private final Integer messageCode;
	@SerializedName("b")
	private final String message;
	@SerializedName("c")
	private FileHandleImpl fileHandle;
	@SerializedName("d")
	private ObjectHandleImpl objectHandle;
	@SerializedName("e")
	private ListHandleImpl listHandle;
	@SerializedName("f")
	private Long trackingId;
	@SerializedName("g")
	private Integer statusResponseFlag;

	public MessageHandleImpl(Integer messageCode, String message) {

		this.messageCode = messageCode;
		this.message = message;

	}

	public MessageHandleImpl(MessageHandle messageHandle) {

		MessageHandleImpl messageHandleImpl = (MessageHandleImpl) messageHandle;

		this.messageCode = messageHandleImpl.messageCode;
		this.message = messageHandleImpl.message;
		this.fileHandle = messageHandleImpl.fileHandle;
		this.objectHandle = messageHandleImpl.objectHandle;
		this.listHandle = messageHandleImpl.listHandle;
		this.trackingId = messageHandleImpl.trackingId;

	}

	public void setTrackingId(Long trackingId) {
		this.trackingId = trackingId;
	}

	public Long getTrackingId() {
		return trackingId;
	}

	public Integer getStatusResponseFlag() {
		return statusResponseFlag;
	}

	public void setStatusResponseFlag(Integer statusResponseFlag) {
		this.statusResponseFlag = statusResponseFlag;
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
	public FileHandle getFileHandle() {

		return fileHandle;

	}

	@Override
	public void setFileHandle(FileHandle fileHandle) {

		this.fileHandle = (FileHandleImpl) fileHandle;

	}

	@Override
	public ObjectHandle getObjectHandle() {

		return objectHandle;

	}

	@Override
	public void setObjectHandle(ObjectHandle objectHandle) {

		this.objectHandle = (ObjectHandleImpl) objectHandle;

	}

	@Override
	public ListHandle getListHandle() {

		return listHandle;

	}

	@Override
	public void setListHandle(ListHandle listHandle) {

		this.listHandle = (ListHandleImpl) listHandle;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static MessageHandleImpl fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, MessageHandleImpl.class);
	}

}
