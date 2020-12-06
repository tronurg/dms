package com.ogya.dms.intf.handles;

import com.ogya.dms.structures.Availability;

public interface ContactHandle {

	Long getId();

	String getUuid();

	String getName();

	String getComment();

	Double getLattitude();

	Double getLongitude();

	Availability getAvailability();

}
