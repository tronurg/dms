package com.ogya.dms.intf;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JComponent;

import com.ogya.dms.intf.handles.ContactHandle;
import com.ogya.dms.intf.handles.ContactSelectionHandle;
import com.ogya.dms.intf.handles.GroupHandle;
import com.ogya.dms.intf.handles.GroupSelectionHandle;
import com.ogya.dms.intf.listeners.DmsGuiListener;
import com.ogya.dms.intf.listeners.DmsListener;

public interface DmsHandle {

	JComponent getDmsPanel();

	void addListener(DmsListener listener);

	void removeListener(DmsListener listener);

	void addGuiListener(DmsGuiListener guiListener);

	void removeGuiListener(DmsGuiListener guiListener);

	void setCoordinates(Double lattitude, Double longitude);

	void setComment(String comment);

	ContactHandle getMyContactHandle();

	GroupSelectionHandle getMyActiveGroupsHandle();

	ContactSelectionHandle getOnlineContactsHandle();

	ContactHandle getContactHandle(Long contactId);

	List<ContactHandle> getAllContactHandles();

	GroupHandle getGroupHandle(Long groupId);

	List<Long> getIdsByAddress(InetAddress address);

	boolean sendMessageToContacts(String message, Integer messageCode, List<Long> contactIds);

	boolean sendMessageToGroup(String message, Integer messageCode, Long groupId);

	boolean sendObjectToContacts(Object object, Integer objectCode, List<Long> contactIds);

	boolean sendObjectToGroup(Object object, Integer objectCode, Long groupId);

	<T> boolean sendListToContacts(List<T> list, Class<T> elementType, Integer listCode, List<Long> contactIds);

	<T> boolean sendListToGroup(List<T> list, Class<T> elementType, Integer listCode, Long groupId);

	boolean sendFileToContacts(Path path, Integer fileCode, List<Long> contactIds);

	boolean sendFileToGroup(Path path, Integer fileCode, Long groupId);

}
