package com.ogya.dms.core.intf.handles;

import java.util.List;

import com.ogya.dms.core.intf.tools.ContactId;
import com.ogya.dms.core.intf.tools.GroupId;
import com.ogya.dms.core.structures.Availability;

public interface GroupHandle {

	GroupId getGroupId();

	GroupId getGroupRefId();

	ContactId getOwnerId();

	String getName();

	String getComment();

	List<ContactId> getContactIds();

	Availability getAvailability();

}
