package com.dms.server.communications.intf;

import java.net.InetAddress;
import java.util.Map;

public interface TcpManagerListener {

	void serverConnectionsUpdated(String dmsUuid, Map<InetAddress, InetAddress> localRemoteIps,
			boolean beaconsRequested);

	void messageReceivedFromRemoteServer(int messageNumber, byte[] data, String dmsUuid);

}
