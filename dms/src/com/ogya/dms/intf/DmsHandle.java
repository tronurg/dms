package com.ogya.dms.intf;

import java.nio.file.Path;
import java.util.List;

import javax.swing.JComponent;

import com.ogya.dms.intf.handles.ContactHandle;
import com.ogya.dms.intf.handles.ContactSelectionHandle;
import com.ogya.dms.intf.handles.GroupHandle;
import com.ogya.dms.intf.handles.GroupSelectionHandle;
import com.ogya.dms.intf.listeners.DmsListener;

public interface DmsHandle {

	JComponent getDmsPanel();

	void addListener(DmsListener listener);

	void removeListener(DmsListener listener);

	void setCoordinates(Double lattitude, Double longitude);

	void setComment(String comment);

	ContactHandle getMyContactHandle();

	GroupSelectionHandle getMyGroupsHandle();

	ContactSelectionHandle getOnlineContactsHandle();

	ContactHandle getContactHandle(String uuid);

	GroupHandle getGroupHandle(String groupUuid);

	boolean sendMessageToContacts(String message, Integer messageCode, List<String> contactUuids);

	boolean sendMessageToGroup(String message, Integer messageCode, String groupUuid);

	boolean sendObjectToContacts(Object object, Integer objectCode, List<String> contactUuids);

	boolean sendObjectToGroup(Object object, Integer objectCode, String groupUuid);

	boolean sendFileToContacts(Path path, Integer fileCode, List<String> contactUuids);

	boolean sendFileToGroup(Path path, Integer fileCode, String groupUuid);

}
