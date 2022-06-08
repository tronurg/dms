package com.ogya.dms.server.communications.intf;

import java.net.InetAddress;
import java.util.Map;

import com.ogya.dms.server.structures.RemoteChunk;

public interface TcpManagerListener {

	void serverConnectionsUpdated(String dmsUuid, Map<InetAddress, InetAddress> localRemoteIps,
			boolean beaconsRequested);

	void messageReceivedFromRemoteServer(int messageNumber, byte[] data, String dmsUuid);

	void sendMoreClaimed(RemoteChunk chunk);

}
