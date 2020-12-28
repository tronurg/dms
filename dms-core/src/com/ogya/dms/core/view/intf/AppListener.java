package com.ogya.dms.core.view.intf;

import java.nio.file.Path;
import java.util.Set;

import com.ogya.dms.core.structures.Availability;

public interface AppListener {

	void commentUpdateRequested(String comment);

	void statusUpdateRequested(Availability availability);

	void messagePaneOpened(Long id);

	void messagePaneClosed(Long id);

	void sendMessageClicked(String messageTxt, Long refMessageId, Long receiverId);

	void showFoldersClicked(Long id);

	void reportClicked(Long id);

	void paneScrolledToTop(Long id, Long topMessageId);

	void showAddUpdateGroupClicked(Long id);

	void addUpdateGroupRequested(String groupName, Set<String> selectedUuids);

	void deleteGroupRequested();

	void showFoldersCanceled();

	void fileSelected(Path file);

	void messageClicked(Long messageId);

	void infoClicked(Long messageId);

	void cancelClicked(Long messageId);

	void statusInfoClosed();

	void addIpClicked(String ip);

	void removeIpClicked(String ip);

	void recordButtonPressed(Long id);

	void recordEventTriggered();

	void recordButtonReleased();

}
