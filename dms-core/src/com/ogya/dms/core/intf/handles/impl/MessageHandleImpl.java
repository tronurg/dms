package com.ogya.dms.core.intf.handles.impl;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.ListHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.intf.handles.ObjectHandle;

public class MessageHandleImpl implements MessageHandle {

	@SerializedName(value = "a")
	private final Integer messageCode;
	@SerializedName(value = "b")
	private final String message;
	@SerializedName(value = "c")
	private FileHandleImpl fileHandle;
	@SerializedName(value = "d")
	private ObjectHandleImpl objectHandle;
	@SerializedName(value = "e")
	private ListHandleImpl listHandle;
	@SerializedName(value = "f")
	private Integer trackingId;
	@SerializedName(value = "g")
	private Integer flag;

	public MessageHandleImpl(Integer messageCode, String message) {

		this.messageCode = messageCode;
		this.message = message;

	}

	public MessageHandleImpl(MessageHandle messageHandle) {

		MessageHandleImpl messageHandleImpl = (MessageHandleImpl) messageHandle;

		this.messageCode = messageHandleImpl.getMessageCode();
		this.message = messageHandleImpl.getMessage();
		this.fileHandle = messageHandleImpl.fileHandle;
		this.objectHandle = messageHandleImpl.objectHandle;
		this.listHandle = messageHandleImpl.listHandle;
		this.trackingId = messageHandleImpl.trackingId;

	}

	public Integer getTrackingId() {
		return trackingId;
	}

	public Integer getFlag() {
		return flag;
	}

	public void setFlag(Integer flag) {
		this.flag = flag;
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
	public void setTrackingId(Integer trackingId) {

		this.trackingId = trackingId;

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
