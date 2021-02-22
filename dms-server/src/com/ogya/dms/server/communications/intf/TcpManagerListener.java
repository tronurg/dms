package com.ogya.dms.server.communications.intf;

import java.net.InetAddress;
import java.util.List;

import com.ogya.dms.commons.structures.MessagePojo;

public interface TcpManagerListener {

	void serverConnectionsUpdated(String dmsUuid, List<InetAddress> remoteAddresses, List<InetAddress> localAddresses);

	void messageReceivedFromRemoteServer(MessagePojo messagePojo, String dmsUuid);

}
