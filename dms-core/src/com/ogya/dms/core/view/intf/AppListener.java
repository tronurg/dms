package com.ogya.dms.core.view.intf;

import java.nio.file.Path;
import java.util.Set;

import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.FileBuilder;

public interface AppListener {

	void commentUpdateRequested(String comment);

	void statusUpdateRequested(Availability availability);

	void messagePaneOpened(Long id);

	void messagePaneClosed();

	void sendMessageClicked(Long id, String messageTxt, FileBuilder fileBuilder, Long refMessageId);

	void reportClicked();

	void paneScrolledToTop(Long id, Long topMessageId);

	void messagesClaimed(Long id, Long lastMessageIdExcl, Long firstMessageIdIncl);

	void showAddUpdateGroupClicked(Long id);

	void addUpdateGroupRequested(String groupName, Set<String> selectedUuids);

	void deleteGroupRequested();

	void fileSelected(Path file);

	void attachmentClicked(Long messageId);

	void infoClicked(Long messageId);

	void deleteMessagesRequested(Long... messageIds);

	void archiveMessagesRequested(Long... messageIds);

	void statusInfoClosed();

	void addIpClicked(String ip);

	void removeIpClicked(String ip);

	void recordButtonPressed();

	void recordEventTriggered(Long id, Long refMessageId);

	void recordButtonReleased();

	void moreArchivedMessagesRequested(Long minMessageId);

}
