package com.dms.core.intf.handles;

import java.net.InetAddress;
import java.util.Map;

import com.dms.core.intf.tools.ContactId;
import com.dms.core.structures.Availability;

public interface ContactHandle {

	ContactId getId();

	String getName();

	String getComment();

	Double getLatitude();

	Double getLongitude();

	Availability getAvailability();

	String getSecretId();

	Map<InetAddress, InetAddress> getLocalRemoteServerIps();

}
