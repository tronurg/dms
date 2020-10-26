package com.ogya.dms.intf.listeners;

import com.ogya.dms.intf.handles.FileHandle;
import com.ogya.dms.intf.handles.MessageHandle;

public interface DmsGuiListener {

	void messageSent(MessageHandle messageHandle);

	void messageReceived(MessageHandle messageHandle);

	void fileSent(FileHandle fileHandle);

	void fileReceived(FileHandle fileHandle);

	void audioSent(FileHandle fileHandle);

	void audioReceived(FileHandle fileHandle);

}
