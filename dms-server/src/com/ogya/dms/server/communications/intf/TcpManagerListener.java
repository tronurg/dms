package com.ogya.dms.server.communications.intf;

import java.net.InetAddress;
import java.util.Map;

import com.ogya.dms.commons.structures.MessagePojo;

public interface TcpManagerListener {

	void serverConnectionsUpdated(String dmsUuid, Map<InetAddress, InetAddress> localRemoteIps);

	void messageReceivedFromRemoteServer(MessagePojo messagePojo, String dmsUuid);

}
