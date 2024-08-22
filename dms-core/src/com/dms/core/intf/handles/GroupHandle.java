package com.dms.core.intf.handles;

import java.util.List;

import com.dms.core.intf.tools.ContactId;
import com.dms.core.intf.tools.GroupId;
import com.dms.core.structures.Availability;

public interface GroupHandle {

	GroupId getGroupId();

	String getName();

	String getComment();

	List<ContactId> getContactIds();

	Availability getAvailability();

}
