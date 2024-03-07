package com.ogya.dms.core.intf.listeners;

import java.nio.file.Path;
import java.util.List;

import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.intf.tools.ContactId;

public interface DmsListener {

	void fileClicked(Path path);

	void messageReceived(MessageHandle messageHandle, ContactId contactId);

	void messageTransmitted(Long trackingId, ContactId contactId);

	void messageFailed(Long trackingId, List<ContactId> contactIds);

	void contactUpdated(ContactHandle contactHandle);

	void groupUpdated(GroupHandle groupHandle);

}
