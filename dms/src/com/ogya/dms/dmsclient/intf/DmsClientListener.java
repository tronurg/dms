package com.ogya.dms.dmsclient.intf;

public interface DmsClientListener {

	void beaconReceived(String message);

	void messageReceived(String message);

	void userDisconnected(String uuid);

	void serverConnStatusUpdated(boolean connStatus);

	void messageStatusClaimed(String message, String remoteUuid);

	void messageNotReceivedRemotely(String message, String remoteUuid);

	void messageReceivedRemotely(String message, String remoteUuid);

	void messageReadRemotely(String message, String remoteUuid);

}
