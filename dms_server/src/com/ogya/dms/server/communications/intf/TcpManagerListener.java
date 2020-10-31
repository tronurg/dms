package com.ogya.dms.server.communications.intf;

import java.net.InetAddress;
import java.util.List;

public interface TcpManagerListener {

	void serverConnectionsUpdated(String dmsUuid, List<InetAddress> addresses);

	void messageReceivedFromRemoteServer(String message, String dmsUuid);

}
