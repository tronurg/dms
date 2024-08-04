package com.dms.server.communications.intf;

import java.net.InetAddress;

public interface UdpManagerListener {

	void udpMessageReceived(InetAddress senderAddress, String message, boolean isUnicast);

}
