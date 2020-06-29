package com.ogya.dms.view.intf;

import java.util.List;

public interface AppListener {

	void commentUpdated(String comment);

	void updateStatusClicked();

	void contactMessagePaneOpened(String uuid);

	void contactMessagePaneClosed(String uuid);

	void sendPrivateMessageClicked(String messageTxt, String receiverUuid);

	void contactPaneScrolledToTop(String uuid);

	void createGroupRequested(String groupName, List<String> selectedUuids);

}
