package com.ogya.dms.core.intf.listeners;

import java.nio.file.Path;

import com.ogya.dms.core.structures.MessageStatus;

public interface DmsGuiListener {

	void guiMessageSent(Long messageId, String message, Long contactId, Long groupId);

	void guiMessageReceived(Long messageId, String message, Long contactId, Long groupId);

	void guiFileSent(Long messageId, String message, Path path, Long contactId, Long groupId);

	void guiFileReceived(Long messageId, String message, Path path, Long contactId, Long groupId);

	void guiReportSent(Long messageId, String message, Integer reportId, Path path, Long contactId, Long groupId);

	void guiReportReceived(Long messageId, String message, Integer reportId, Path path, Long contactId, Long groupId);

	void guiAudioSent(Long messageId, Path path, Long contactId, Long groupId);

	void guiAudioReceived(Long messageId, Path path, Long contactId, Long groupId);

	void guiMessageStatusUpdated(Long messageId, MessageStatus messageStatus, Long contactId);

	void guiMessagesDeleted(Long... messageIds);

	void guiPrivateConversationDeleted(Long contactId, Long[] deletedMessageIds);

	void guiGroupConversationDeleted(Long groupId, Long[] deletedMessageIds);

}
