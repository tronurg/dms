package com.ogya.dms.server.communications.intf;

import java.net.InetAddress;
import java.util.Map;

import com.ogya.dms.server.structures.Chunk;

public interface TcpManagerListener {

	void serverConnectionsUpdated(String dmsUuid, Map<InetAddress, InetAddress> localRemoteIps,
			boolean beaconsRequested);

	void messageReceivedFromRemoteServer(int messageNumber, byte[] data, String dmsUuid);

	void sendMoreClaimed(Chunk chunk);

}
