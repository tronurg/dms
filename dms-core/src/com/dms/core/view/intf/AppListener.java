package com.dms.core.view.intf;

import java.nio.file.Path;
import java.util.Set;

import com.dms.core.database.tables.EntityId;
import com.dms.core.structures.Availability;
import com.dms.core.structures.FileBuilder;

public interface AppListener {

	void commentUpdateRequested(String comment);

	void statusUpdateRequested(Availability availability);

	void messagePaneOpened(EntityId entityId);

	void messagePaneClosed();

	void sendMessageClicked(String messageTxt, FileBuilder fileBuilder, Long refMessageId);

	void reportClicked();

	void paneScrolledToTop(Long topMessageId);

	void messagesClaimed(Long lastMessageIdExcl, Long firstMessageIdIncl);

	void showAddUpdateGroupClicked();

	void addUpdateGroupRequested(String groupName, Set<Long> selectedIds);

	void deleteGroupRequested();

	void fileSelected(Path file);

	void attachmentClicked(Long messageId);

	void infoClicked(Long messageId);

	void forwardMessagesRequested(EntityId entityId, Long[] messageIds);

	void archiveMessagesRequested(Long[] messageIds);

	void deleteMessagesRequested(Long[] messageIds);

	void clearConversationRequested();

	void statusInfoClosed();

	void addIpClicked(String ip);

	void removeIpClicked(String ip);

	void recordButtonPressed();

	void recordEventTriggered(Long refMessageId);

	void recordButtonReleased();

	void showEntityRequested(EntityId entityId);

	void hideEntityRequested(EntityId entityId);

	void removeEntityRequested(EntityId entityId);

	void moreArchivedMessagesRequested(Long minMessageId);

	void searchRequested(String fulltext);

	void archiveSearchRequested(String fulltext);

	void archivedMessagesClaimed(Long lastMessageIdExcl, Long firstMessageIdIncl);

	void searchInAllMessagesClaimed(String fulltext);

}
