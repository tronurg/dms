package com.ogya.dms.core.intf.handles;

import com.ogya.dms.core.structures.Availability;

public interface ContactHandle {

	Long getId();

	String getUuid();

	String getName();

	String getComment();

	Double getLattitude();

	Double getLongitude();

	Availability getAvailability();

}
