package com.ogya.dms.core.intf.listeners;

import java.nio.file.Path;

public interface DmsGuiListener {

	void guiMessageSent(String message, Long contactId, Long groupId);

	void guiMessageReceived(String message, Long contactId, Long groupId);

	void guiFileSent(Path path, Long contactId, Long groupId);

	void guiFileReceived(Path path, Long contactId, Long groupId);

	void guiReportSent(Path path, Long contactId, Long groupId);

	void guiReportReceived(Path path, Long contactId, Long groupId);

	void guiAudioSent(Path path, Long contactId, Long groupId);

	void guiAudioReceived(Path path, Long contactId, Long groupId);

}
