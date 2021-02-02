package com.ogya.dms.core.intf.listeners;

import java.nio.file.Path;
import java.util.List;

import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.structures.MessageStatus;

public interface DmsListener {

	void fileClicked(Path path);

	void messageReceived(MessageHandle messageHandle, Long contactId, Long groupId);

	void messageTransmitted(Long trackingId, Long contactId);

	void messageFailed(Long trackingId, List<Long> contactIds);

	void guiMessageStatusUpdated(Long messageId, MessageStatus messageStatus, Long contactId);

	void contactUpdated(ContactHandle contactHandle);

	void groupUpdated(GroupHandle groupHandle);

}
