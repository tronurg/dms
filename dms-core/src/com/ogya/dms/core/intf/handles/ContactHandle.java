package com.ogya.dms.core.intf.handles;

import java.net.InetAddress;
import java.util.List;

import com.ogya.dms.core.structures.Availability;

public interface ContactHandle {

	Long getId();

	String getUuid();

	String getName();

	String getComment();

	Double getLattitude();

	Double getLongitude();

	Availability getAvailability();

	List<InetAddress> getRemoteInterfaces();

	List<InetAddress> getLocalInterfaces();

}
