package com.ogya.dms.core.intf.listeners;

import java.nio.file.Path;

import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.structures.MessageStatus;

public interface DmsListener {

	void fileClicked(Path path);

	void messageReceived(MessageHandle messageHandle, Long contactId, Long groupId);

	void messageTransmitted(Integer trackingId, Long contactId);

	void guiMessageStatusUpdated(Long messageId, MessageStatus messageStatus);

	void contactUpdated(ContactHandle contactHandle);

	void groupUpdated(GroupHandle groupHandle);

}
