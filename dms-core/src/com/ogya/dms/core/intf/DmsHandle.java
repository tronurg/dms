package com.ogya.dms.core.intf;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import javax.swing.JComponent;

import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.ContactSelectionHandle;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.GroupSelectionHandle;
import com.ogya.dms.core.intf.handles.ListHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.intf.handles.ObjectHandle;
import com.ogya.dms.core.intf.listeners.DmsDownloadListener;
import com.ogya.dms.core.intf.listeners.DmsFileServer;
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

	ContactHandle getContactHandle(Long contactId);

	GroupHandle getGroupHandle(Long groupId);

	List<ContactHandle> getAllContactHandles();

	List<GroupHandle> getAllGroupHandles();

	List<ContactHandle> getContactHandles(Predicate<ContactHandle> filter);

	List<GroupHandle> getGroupHandles(Predicate<GroupHandle> filter);

	List<Long> getIdsByServerIp(InetAddress remoteServerIp);

	List<Long> getIdsByServerIpAndName(InetAddress remoteServerIp, String name);

	List<Long> getIdsByServerIpAndSecretId(InetAddress remoteServerIp, String secretId);

	MessageHandle createMessageHandle(String message, Integer messageCode);

	FileHandle createFileHandle(Path path, Integer fileCode);

	ObjectHandle createObjectHandle(Object object, Integer objectCode);

	<T> ListHandle createListHandle(List<T> list, Integer listCode);

	MessageRules createMessageRules();

	Long sendMessageToContacts(MessageHandle messageHandle, List<Long> contactIds);

	Long sendMessageToGroup(MessageHandle messageHandle, Long groupId);

	Long sendMessageToContacts(MessageHandle messageHandle, List<Long> contactIds, MessageRules messageRules);

	Long sendMessageToGroup(MessageHandle messageHandle, Long groupId, MessageRules messageRules);

	void cancelMessage(Long trackingId);

	Long downloadFile(Long fileId, Long contactId);

	void cancelDownload(Long downloadId);

	void pauseDownload(Long downloadId);

	void resumeDownload(Long downloadId);

	Future<Long> sendGuiMessageToContact(String message, Long contactId);

	Future<Long> sendGuiMessageToGroup(String message, Long groupId);

	Future<Long> sendGuiFileToContact(String message, Path path, Long contactId);

	Future<Long> sendGuiFileToGroup(String message, Path path, Long groupId);

	Future<Long> sendGuiReportToContact(String message, Integer reportId, Path path, Long contactId);

	Future<Long> sendGuiReportToGroup(String message, Integer reportId, Path path, Long groupId);

	void clearGuiPrivateConversation(Long id);

	void clearGuiGroupConversation(Long id);

	void switchAudio(boolean on);

	boolean isServerConnected();

}
