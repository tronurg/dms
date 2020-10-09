package com.ogya.dms.intf.handles;

public interface MessageHandle {

	Integer getMessageCode();

	String getMessage();

	String getSenderUuid();

	String getGroupUuid();

}
