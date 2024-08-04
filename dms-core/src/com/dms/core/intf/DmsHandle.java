package com.dms.core.intf;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import javax.swing.JComponent;

import com.dms.core.intf.handles.ContactHandle;
import com.dms.core.intf.handles.ContactSelectionHandle;
import com.dms.core.intf.handles.FileHandle;
import com.dms.core.intf.handles.GroupHandle;
import com.dms.core.intf.handles.GroupSelectionHandle;
import com.dms.core.intf.handles.ListHandle;
import com.dms.core.intf.handles.MessageHandle;
import com.dms.core.intf.handles.ObjectHandle;
import com.dms.core.intf.listeners.DmsDownloadListener;
import com.dms.core.intf.listeners.DmsFileServer;
import com.dms.core.intf.listeners.DmsGuiListener;
import com.dms.core.intf.listeners.DmsListener;
import com.dms.core.intf.tools.ContactId;
import com.dms.core.intf.tools.GroupId;
import com.dms.core.intf.tools.MessageRules;
import com.dms.core.structures.Availability;

public interface DmsHandle {

	JComponent getDmsPanel();

	void logout();

	void addListener(DmsListener listener);

	void removeListener(DmsListener listener);

	void addGuiListener(DmsGuiListener guiListener);

	void removeGuiListener(DmsGuiListener guiListener);

	void addDownloadListener(DmsDownloadListener downloadListener);

	void removeDownloadListener(DmsDownloadListener downloadListener);

	void registerFileServer(DmsFileServer fileServer);

	void unregisterFileServer();

	void setCoordinates(Double latitude, Double longitude) throws UnsupportedOperationException;

	void setComment(String comment) throws UnsupportedOperationException;

	void setAvailability(Availability availability) throws UnsupportedOperationException;

	void setSecretId(String secretId) throws UnsupportedOperationException;

	void addRemoteIps(InetAddress... remoteIps);

	void clearRemoteIps();

	ContactHandle getMyContactHandle();

	GroupSelectionHandle getActiveGroupsHandle();

	ContactSelectionHandle getActiveContactsHandle();

	ContactHandle getContactHandle(ContactId contactId);

	GroupHandle getGroupHandle(GroupId groupId);

	List<ContactHandle> getAllContactHandles();

	List<GroupHandle> getAllGroupHandles();

	List<ContactHandle> getContactHandles(Predicate<ContactHandle> filter);

	List<GroupHandle> getGroupHandles(Predicate<GroupHandle> filter);

	List<ContactId> getIdsByServerIp(InetAddress remoteServerIp);

	List<ContactId> getIdsByServerIpAndName(InetAddress remoteServerIp, String name);

	List<ContactId> getIdsByServerIpAndSecretId(InetAddress remoteServerIp, String secretId);

	int getUnreadMessagesCount();

	MessageHandle createMessageHandle(String message, Integer messageCode);

	FileHandle createFileHandle(Path path, Integer fileCode);

	ObjectHandle createObjectHandle(Object object, Integer objectCode);

	<T> ListHandle createListHandle(List<T> list, Integer listCode);

	MessageRules createMessageRules();

	boolean sendMessageToContacts(MessageHandle messageHandle, List<ContactId> contactIds);

	boolean sendMessageToGroup(MessageHandle messageHandle, GroupId groupId);

	boolean sendMessageToContacts(MessageHandle messageHandle, List<ContactId> contactIds, MessageRules messageRules);

	boolean sendMessageToGroup(MessageHandle messageHandle, GroupId groupId, MessageRules messageRules);

	void cancelMessage(Long trackingId);

	Long downloadFile(Long fileId, ContactId contactId);

	void cancelDownload(Long downloadId);

	void pauseDownload(Long downloadId);

	void resumeDownload(Long downloadId);

	Future<Long> sendGuiMessageToContact(String message, ContactId contactId);

	Future<Long> sendGuiMessageToGroup(String message, GroupId groupId);

	Future<Long> sendGuiFileToContact(String message, Path path, ContactId contactId);

	Future<Long> sendGuiFileToGroup(String message, Path path, GroupId groupId);

	Future<Long> sendGuiReportToContact(String message, Integer reportId, Path path, ContactId contactId);

	Future<Long> sendGuiReportToGroup(String message, Integer reportId, Path path, GroupId groupId);

	void clearGuiPrivateConversation(ContactId contactId);

	void clearGuiGroupConversation(GroupId groupId);

	void markGuiPrivateMessagesAsRead(ContactId contactId);

	void markGuiGroupMessagesAsRead(GroupId groupId);

	void switchAudio(boolean on);

	boolean isServerConnected();

}
