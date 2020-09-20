package com.ogya.dms.dmsclient.intf;

import com.ogya.dms.structures.MessageStatus;

public interface DmsClientListener {

	void beaconReceived(String message);

	void remoteIpsReceived(String message);

	void progressReceived(Long messageId, String[] remoteUuids, int progress);

	void messageReceived(String message, String remoteUuid);

	void userDisconnected(String uuid);

	void serverConnStatusUpdated(boolean connStatus);

	void messageStatusClaimed(Long messageId, String remoteUuid);

	void messageStatusFed(Long messageId, MessageStatus messageStatus, String[] remoteUuids);

	void statusReportClaimed(Long messageId, String remoteUuid);

	void statusReportFed(Long messageId, String message, String remoteUuid);

	void transientMessageReceived(String message, String remoteUuid);

}
