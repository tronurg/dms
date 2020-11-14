package com.ogya.dms.intf.handles;

import java.util.List;

public interface GroupHandle {

	Long getGroupId();

	String getName();

	String getComment();

	List<Long> getContactIds();

}
