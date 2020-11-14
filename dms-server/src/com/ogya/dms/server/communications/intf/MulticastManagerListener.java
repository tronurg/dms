package com.ogya.dms.server.communications.intf;

import java.net.InetAddress;

public interface MulticastManagerListener {

	void udpMessageReceived(InetAddress senderAddress, String message, boolean isUnicast);

}
