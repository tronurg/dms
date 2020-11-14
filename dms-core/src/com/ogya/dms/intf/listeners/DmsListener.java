package com.ogya.dms.intf.listeners;

import java.nio.file.Path;

import com.ogya.dms.intf.handles.ContactHandle;
import com.ogya.dms.intf.handles.FileHandle;
import com.ogya.dms.intf.handles.ListHandle;
import com.ogya.dms.intf.handles.MessageHandle;
import com.ogya.dms.intf.handles.ObjectHandle;

public interface DmsListener {

	void fileClicked(Path path);

	void messageReceived(MessageHandle messageHandle);

	void objectReceived(ObjectHandle objectHandle);

	void listReceived(ListHandle listHandle);

	void fileReceived(FileHandle fileHandle);

	void contactUpdated(ContactHandle contactHandle);

}
