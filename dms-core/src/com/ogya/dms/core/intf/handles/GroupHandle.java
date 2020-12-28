package com.ogya.dms.core.intf.handles;

import java.util.List;

import com.ogya.dms.core.structures.Availability;

public interface GroupHandle {

	Long getGroupId();

	Long getGroupRefId();

	Long getOwnerId();

	String getName();

	String getComment();

	List<Long> getContactIds();

	Availability getAvailability();

}
