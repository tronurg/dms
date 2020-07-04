package com.ogya.dms.view.intf;

import java.util.Set;

public interface AppListener {

	void commentUpdated(String comment);

	void updateStatusClicked();

	void contactMessagePaneOpened(String uuid);

	void contactMessagePaneClosed(String uuid);

	void sendPrivateMessageClicked(String messageTxt, String receiverUuid);

	void contactPaneScrolledToTop(String uuid);

	void showAddUpdateGroupClicked(String groupUuid);

	void addUpdateGroupRequested(String groupName, Set<String> selectedUuids);

	void groupMessagePaneOpened(String groupUuid);

	void groupMessagePaneClosed(String groupUuid);

	void sendGroupMessageClicked(String messageTxt, String groupUuid);

	void groupPaneScrolledToTop(String groupUuid);

}
