package com.ogya.dms.core.intf.listeners;

import java.nio.file.Path;

import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.ListHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.intf.handles.ObjectHandle;

public interface DmsListener {

	void fileClicked(Path path);

	void messageReceived(MessageHandle messageHandle);

	void objectReceived(ObjectHandle objectHandle);

	void listReceived(ListHandle listHandle);

	void fileReceived(FileHandle fileHandle);

	void contactUpdated(ContactHandle contactHandle);

	void groupUpdated(GroupHandle groupHandle);

}
