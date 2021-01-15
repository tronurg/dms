package com.ogya.dms.core.intf;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JComponent;

import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.ContactSelectionHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.GroupSelectionHandle;
import com.ogya.dms.core.intf.listeners.DmsGuiListener;
import com.ogya.dms.core.intf.listeners.DmsListener;
import com.ogya.dms.core.structures.Availability;

public interface DmsHandle {

	JComponent getDmsPanel();

	void addListener(DmsListener listener);

	void removeListener(DmsListener listener);

	void addGuiListener(DmsGuiListener guiListener);

	void removeGuiListener(DmsGuiListener guiListener);

	void setCoordinates(Double lattitude, Double longitude);

	void setComment(String comment);

	void setAvailability(Availability availability);

	ContactHandle getMyContactHandle();

	GroupSelectionHandle getActiveGroupsHandle();

	ContactSelectionHandle getOnlineContactsHandle();

	ContactHandle getContactHandle(Long contactId);

	GroupHandle getGroupHandle(Long groupId);

	List<ContactHandle> getAllContactHandles();

	List<GroupHandle> getAllGroupHandles();

	List<Long> getIdsByAddress(InetAddress address);

	List<Long> getIdsByAddressAndName(InetAddress address, String name);

	boolean sendMessageToContacts(String message, Integer messageCode, List<Long> contactIds);

	boolean sendMessageToGroup(String message, Integer messageCode, Long groupId);

	boolean sendObjectToContacts(Object object, Integer objectCode, List<Long> contactIds);

	boolean sendObjectToGroup(Object object, Integer objectCode, Long groupId);

	<T> boolean sendListToContacts(List<T> list, Class<T> elementType, Integer listCode, List<Long> contactIds);

	<T> boolean sendListToGroup(List<T> list, Class<T> elementType, Integer listCode, Long groupId);

	boolean sendFileToContacts(Path path, Integer fileCode, List<Long> contactIds);

	boolean sendFileToGroup(Path path, Integer fileCode, Long groupId);

}
