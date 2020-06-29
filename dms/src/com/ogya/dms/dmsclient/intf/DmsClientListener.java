package com.ogya.dms.dmsclient.intf;

public interface DmsClientListener {

	void beaconReceived(String message);

	void messageReceived(String message, String remoteUuid);

	void userDisconnected(String uuid);

	void serverConnStatusUpdated(boolean connStatus);

	void messageStatusClaimed(String message, String remoteUuid);

	void messageStatusFed(String message, String remoteUuid);

	void statusReportClaimed(String message, String remoteUuid);

	void statusReportFed(String message, String remoteUuid);

}
