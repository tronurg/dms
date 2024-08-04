package com.dms.core.dmsclient.intf;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.dms.commons.structures.Beacon;
import com.dms.core.database.tables.Message;
import com.dms.core.database.tables.StatusReport;
import com.dms.core.intf.handles.impl.MessageHandleImpl;
import com.dms.core.structures.DownloadPojo;
import com.dms.core.structures.GroupMessageStatus;
import com.dms.core.structures.MessageStatus;

public interface DmsClientListener {

	void beaconReceived(Beacon beacon);

	void remoteIpsReceived(InetAddress[] remoteIps);

	void progressMessageReceived(Long messageId, List<String> remoteUuids, int progress);

	void progressTransientReceived(Long trackingId, List<String> remoteUuids, int progress);

	void messageReceived(Message message, Path attachment, String remoteUuid);

	void userDisconnected(String uuid);

	void serverConnStatusUpdated(boolean connStatus, boolean logout);

	void messageStatusClaimed(Long[] messageIds, String remoteUuid);

	void messageStatusFed(Map<Long, MessageStatus> messageIdStatusMap, String remoteUuid);

	void groupMessageStatusFed(Map<Long, GroupMessageStatus> messageIdGroupStatusMap, String remoteUuid);

	void statusReportClaimed(Long[] messageIds, String remoteUuid);

	void statusReportFed(Map<Long, StatusReport[]> messageIdStatusReportsMap);

	void transientMessageReceived(MessageHandleImpl message, Path attachment, String remoteUuid, Long trackingId);

	void transientMessageStatusReceived(Long trackingId, String remoteUuid);

	void downloadRequested(DownloadPojo downloadPojo, String remoteUuid);

	void cancelDownloadRequested(Long downloadId, String remoteUuid);

	void serverNotFound(Long downloadId);

	void fileNotFound(Long downloadId);

	void downloadingFile(Long downloadId, int progress);

	void fileDownloaded(Long downloadId, Path path, String fileName, boolean partial);

	void downloadFailed(Long downloadId);

}
