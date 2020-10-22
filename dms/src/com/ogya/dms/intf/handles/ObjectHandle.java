package com.ogya.dms.intf.handles;

public interface ObjectHandle {

	Integer getObjectCode();

	<T> T getObject(Class<T> objectClass);

	String getContactUuid();

	String getGroupUuid();

}
