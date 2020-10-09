package com.ogya.dms.intf.handles;

import java.util.List;

public interface GroupHandle {

	String getGroupUuid();

	String getName();

	String getComment();

	List<String> getContactUuids();

}
