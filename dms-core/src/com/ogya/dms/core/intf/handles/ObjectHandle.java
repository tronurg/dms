package com.ogya.dms.core.intf.handles;

public interface ObjectHandle {

	Integer getObjectCode();

	<T> T getObject(Class<T> objectClass);

	Long getContactId();

	Long getGroupId();

}
