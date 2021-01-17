package com.ogya.dms.core.intf.handles;

public interface MessageHandle {

	Integer getMessageCode();

	String getMessage();

	FileHandle getFileHandle();

	void setFileHandle(FileHandle fileHandle);

	ObjectHandle getObjectHandle();

	void setObjectHandle(ObjectHandle objectHandle);

	ListHandle getListHandle();

	void setListHandle(ListHandle listHandle);

}
