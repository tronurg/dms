package com.dms.core.intf.listeners;

import java.nio.file.Path;

import com.dms.core.intf.tools.ContactId;
import com.dms.core.intf.tools.GroupId;
import com.dms.core.structures.MessageStatus;

public interface DmsGuiListener {

	void guiMessageSent(Long messageId, String message, ContactId contactId, GroupId groupId);

	void guiMessageReceived(Long messageId, String message, ContactId contactId, GroupId groupId);

	void guiFileSent(Long messageId, String message, Path path, ContactId contactId, GroupId groupId);

	void guiFileReceived(Long messageId, String message, Path path, ContactId contactId, GroupId groupId);

	void guiReportSent(Long messageId, String message, Integer reportId, Path path, ContactId contactId,
			GroupId groupId);

	void guiReportReceived(Long messageId, String message, Integer reportId, Path path, ContactId contactId,
			GroupId groupId);

	void guiAudioSent(Long messageId, Path path, ContactId contactId, GroupId groupId);

	void guiAudioReceived(Long messageId, Path path, ContactId contactId, GroupId groupId);

	void guiMessageStatusUpdated(Long messageId, MessageStatus messageStatus, ContactId contactId);

	void guiMessagesRead(Long[] messageIds);

	void guiMessagesDeleted(Long[] messageIds);

	void guiPrivateConversationCleared(ContactId contactId, Long[] deletedMessageIds);

	void guiGroupConversationCleared(GroupId groupId, Long[] deletedMessageIds);

	void guiPrivateConversationDeleted(ContactId contactId, Long[] deletedMessageIds);

	void guiGroupConversationDeleted(GroupId groupId, Long[] deletedMessageIds);

}
