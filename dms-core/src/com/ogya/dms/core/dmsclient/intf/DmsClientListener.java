package com.ogya.dms.core.dmsclient.intf;

import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.database.tables.StatusReport;
import com.ogya.dms.core.intf.handles.impl.MessageHandleImpl;
import com.ogya.dms.core.structures.GroupMessageStatus;
import com.ogya.dms.core.structures.MessageStatus;

public interface DmsClientListener {

	void beaconReceived(Beacon beacon);

	void remoteIpsReceived(String[] remoteIps);

	void progressMessageReceived(Long messageId, String[] remoteUuids, int progress);

	void progressTransientReceived(Long trackingId, String[] remoteUuids, int progress);

	void messageReceived(Message message, String remoteUuid);

	void userDisconnected(String uuid);

	void serverConnStatusUpdated(boolean connStatus);

	void messageStatusClaimed(Long messageId, String remoteUuid);

	void messageStatusFed(Long messageId, MessageStatus messageStatus, String remoteUuid);

	void groupMessageStatusFed(Long messageId, GroupMessageStatus groupMessageStatus, String remoteUuid);

	void statusReportClaimed(Long messageId, String remoteUuid);

	void statusReportFed(Long messageId, StatusReport[] statusReports);

	void transientMessageReceived(MessageHandleImpl message, String remoteUuid);

}
