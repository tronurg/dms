package com.ogya.dms.core.intf.handles;

import java.net.InetAddress;
import java.util.Map;

import com.ogya.dms.core.intf.tools.ContactId;
import com.ogya.dms.core.structures.Availability;

public interface ContactHandle {

	ContactId getId();

	String getUuid();

	String getName();

	String getComment();

	Double getLatitude();

	Double getLongitude();

	Availability getAvailability();

	String getSecretId();

	Map<InetAddress, InetAddress> getLocalRemoteServerIps();

}
