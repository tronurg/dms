package com.ogya.dms.server.communications.intf;

import java.net.InetAddress;
import java.util.List;

public interface TcpManagerListener {

	void serverConnectionsUpdated(String dmsUuid, List<InetAddress> remoteAddresses, List<InetAddress> localAddresses);

	void messageReceivedFromRemoteServer(String message, String dmsUuid);

}
