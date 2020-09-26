package com.ogya.dms.server.communications.intf;

public interface TcpManagerListener {

	void connectedToRemoteServer(String dmsUuid);

	void remoteServerDisconnected(String dmsUuid);

	void messageReceivedFromRemoteServer(String message, String dmsUuid);

}
