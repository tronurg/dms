package com.ogya.dms.core.intf.listeners;

import java.nio.file.Path;
import java.util.List;

import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;

public interface DmsListener {

	void fileClicked(Path path);

	void messageReceived(MessageHandle messageHandle, Long contactId);

	void sendingMessage(Long trackingId, List<Long> contactIds, int progress);

	void messageTransmitted(Long trackingId, Long contactId);

	void messageFailed(Long trackingId, List<Long> contactIds);

	void contactUpdated(ContactHandle contactHandle);

	void groupUpdated(GroupHandle groupHandle);

}
