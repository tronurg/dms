package com.ogya.dms.core.intf.handles;

public interface MessageHandle {

	Integer getMessageCode();

	String getMessage();

	Long getContactId();

	Long getGroupId();

}
