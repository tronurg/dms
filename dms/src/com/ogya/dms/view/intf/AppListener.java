package com.ogya.dms.view.intf;

import java.nio.file.Path;
import java.util.Set;

import com.ogya.dms.structures.ReceiverType;

public interface AppListener {

	void commentUpdated(String comment);

	void updateStatusClicked();

	void messagePaneOpened(String uuid, ReceiverType receiverType);

	void messagePaneClosed(String uuid);

	void sendMessageClicked(String messageTxt, String receiverUuid, ReceiverType receiverType);

	void showFoldersClicked(String uuid, ReceiverType receiverType);

	void reportClicked(String uuid, ReceiverType receiverType);

	void paneScrolledToTop(String uuid, ReceiverType receiverType);

	void showAddUpdateGroupClicked(String groupUuid);

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

	void recordButtonPressed(String uuid, ReceiverType receiverType);

	void recordEventTriggered();

	void recordButtonReleased();

}
