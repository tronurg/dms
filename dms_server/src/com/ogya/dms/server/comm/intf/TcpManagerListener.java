package com.ogya.dms.server.comm.intf;

public interface TcpManagerListener {

	void connectedToRemoteServer(String dmsUuid);

	void remoteUserDisconnected(String uuid);

	void messageReceived(String message);

}
