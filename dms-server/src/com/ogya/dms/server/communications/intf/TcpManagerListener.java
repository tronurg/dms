package com.ogya.dms.server.communications.intf;

import java.net.InetAddress;
import java.util.Map;

import com.ogya.dms.commons.structures.MessagePojo;

public interface TcpManagerListener {

	void serverConnectionsUpdated(String dmsUuid, Map<InetAddress, InetAddress> localRemoteIps,
			boolean beaconsRequested);

	void messageReceivedFromRemoteServer(MessagePojo messagePojo, String dmsUuid);

	void downloadProgress(String receiverUuid, Long trackingId, int progress);

}
