package com.ogya.dms.core.intf;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.JComponent;

import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.ContactSelectionHandle;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.GroupSelectionHandle;
import com.ogya.dms.core.intf.handles.ListHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.intf.handles.ObjectHandle;
import com.ogya.dms.core.intf.listeners.DmsGuiListener;
import com.ogya.dms.core.intf.listeners.DmsListener;
import com.ogya.dms.core.intf.tools.MessageRules;
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

	void setSecretId(String secretId);

	void addRemoteIps(InetAddress... remoteIps);

	void clearRemoteIps();

	ContactHandle getMyContactHandle();

	GroupSelectionHandle getActiveGroupsHandle();

	ContactSelectionHandle getOnlineContactsHandle();

	ContactHandle getContactHandle(Long contactId);

	GroupHandle getGroupHandle(Long groupId);

	List<ContactHandle> getAllContactHandles();

	List<GroupHandle> getAllGroupHandles();

	List<Long> getIdsByAddress(InetAddress address);

	List<Long> getIdsByAddressAndName(InetAddress address, String name);

	List<Long> getIdsByAddressAndSecretId(InetAddress address, String secretId);

	MessageHandle createMessageHandle(String message, Integer messageCode);

	FileHandle createFileHandle(Path path, Integer fileCode);

	ObjectHandle createObjectHandle(Object object, Integer objectCode);

	<T> ListHandle createListHandle(List<T> list, Class<T> elementType, Integer listCode);

	MessageRules createMessageRules();

	boolean sendMessageToContacts(MessageHandle messageHandle, List<Long> contactIds);

	boolean sendMessageToGroup(MessageHandle messageHandle, Long groupId);

	boolean sendMessageToContacts(MessageHandle messageHandle, List<Long> contactIds, MessageRules messageRules);

	boolean sendMessageToGroup(MessageHandle messageHandle, Long groupId, MessageRules messageRules);

	Future<Long> sendGuiMessageToContact(String message, Long contactId);

	Future<Long> sendGuiMessageToGroup(String message, Long groupId);

	Future<Long> sendGuiFileToContact(Path path, Long contactId);

	Future<Long> sendGuiFileToGroup(Path path, Long groupId);

	Future<Long> sendGuiReportToContact(Integer reportId, Path path, Long contactId);

	Future<Long> sendGuiReportToGroup(Integer reportId, Path path, Long groupId);

}
