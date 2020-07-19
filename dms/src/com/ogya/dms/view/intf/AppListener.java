package com.ogya.dms.view.intf;

import java.nio.file.Path;
import java.util.Set;

public interface AppListener {

	void commentUpdated(String comment);

	void updateStatusClicked();

	void contactMessagePaneOpened(String uuid);

	void contactMessagePaneClosed(String uuid);

	void sendPrivateMessageClicked(String messageTxt, String receiverUuid);

	void privateShowFoldersClicked(String uuid);

	void contactPaneScrolledToTop(String uuid);

	void showAddUpdateGroupClicked(String groupUuid);

	void addUpdateGroupRequested(String groupName, Set<String> selectedUuids);

	void deleteGroupRequested();

	void groupMessagePaneOpened(String groupUuid);

	void groupMessagePaneClosed(String groupUuid);

	void sendGroupMessageClicked(String messageTxt, String groupUuid);

	void groupShowFoldersClicked(String groupUuid);

	void groupPaneScrolledToTop(String groupUuid);

	void showFoldersCanceled();

	void fileSelected(Path file);

	void messageClicked(Long messageId);

	void infoClicked(Long messageId);

	void cancelClicked(Long messageId);

	void statusInfoClosed();

}
