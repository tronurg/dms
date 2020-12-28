package com.ogya.dms.core.intf.listeners;

import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;

public interface DmsGuiListener {

	void guiMessageSent(MessageHandle messageHandle);

	void guiMessageReceived(MessageHandle messageHandle);

	void guiFileSent(FileHandle fileHandle);

	void guiFileReceived(FileHandle fileHandle);

	void guiReportSent(FileHandle fileHandle);

	void guiReportReceived(FileHandle fileHandle);

	void guiAudioSent(FileHandle fileHandle);

	void guiAudioReceived(FileHandle fileHandle);

}
