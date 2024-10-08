package com.dms.test.main;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.dms.core.intf.DmsHandle;
import com.dms.core.intf.handles.ContactHandle;
import com.dms.core.intf.handles.FileHandle;
import com.dms.core.intf.handles.GroupHandle;
import com.dms.core.intf.handles.ListHandle;
import com.dms.core.intf.handles.MessageHandle;
import com.dms.core.intf.handles.ObjectHandle;
import com.dms.core.intf.listeners.DmsDownloadListener;
import com.dms.core.intf.listeners.DmsFileServer;
import com.dms.core.intf.listeners.DmsGuiListener;
import com.dms.core.intf.listeners.DmsListener;
import com.dms.core.intf.tools.ContactId;
import com.dms.core.intf.tools.GroupId;
import com.dms.core.structures.MessageStatus;

public class DmsListenerImpl implements DmsListener, DmsGuiListener, DmsDownloadListener, DmsFileServer {

	private final DmsHandle dmsHandle;
	private final String myName;

	public DmsListenerImpl(DmsHandle dmsHandle) {

		this.dmsHandle = dmsHandle;
		this.myName = dmsHandle.getMyContactHandle().getName();

	}

	@Override
	public void fileClicked(Path path) {

		System.out.println("File clicked!");

		try {

			new ProcessBuilder().directory(path.getParent().toFile())
					.command("cmd", "/C", path.getFileName().toString()).start();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void contactUpdated(ContactHandle contactHandle) {

		System.out.println(String.format("Kisi guncellendi: %s\n", contactHandle.getName()));

	}

	@Override
	public void groupUpdated(GroupHandle groupHandle) {

		System.out.println(String.format("Grup guncellendi: %s\n", groupHandle.getName()));

	}

	@Override
	public void messageReceived(MessageHandle messageHandle, ContactId contactId) {

		System.out.println(String.format("%s: Message received from: %s\nContent: %s\n", myName,
				dmsHandle.getContactHandle(contactId).getName(), messageHandle.getMessage()));

		FileHandle fileHandle = messageHandle.getFileHandle();
		if (fileHandle != null) {
			System.out.println(String.format("%s: File received from: %s\nContent: %s\n", myName,
					dmsHandle.getContactHandle(contactId).getName(), fileHandle.getPath()));
			try {
				new ProcessBuilder().directory(fileHandle.getPath().getParent().toFile())
						.command("cmd", "/C", fileHandle.getPath().getFileName().toString()).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		ObjectHandle objectHandle = messageHandle.getObjectHandle();
		if (objectHandle != null) {
			System.out.println(String.format("%s: Object received from: %s\nContent: %s\n", myName,
					dmsHandle.getContactHandle(contactId).getName(), objectHandle.getObject(TestPojo.class)));
		}

		ListHandle listHandle = messageHandle.getListHandle();
		if (listHandle != null) {
			System.out.println(String.format("%s: List received from: %s\nContent: %s\n", myName,
					dmsHandle.getContactHandle(contactId).getName(), listHandle.getList(TestPojo.class)));
		}

		try {
			dmsHandle.sendMessageToContacts(messageHandle,
					dmsHandle.getIdsByServerIpAndName(InetAddress.getByName("192.168.1.88"), "elma"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void messageTransmitted(Long trackingId, ContactId contactId) {

		System.out.println(String.format("%s: Message #%d transmitted to %s\n", myName, trackingId,
				dmsHandle.getContactHandle(contactId).getName()));

	}

	@Override
	public void messageFailed(Long trackingId, List<ContactId> contactIds) {

		System.out.println(String.format("%s: Message #%d failed to %s\n", myName, trackingId,
				contactIds.stream().map(contactId -> dmsHandle.getContactHandle(contactId).getName())
						.collect(Collectors.toList()).toString()));

	}

	@Override
	public void guiMessageStatusUpdated(Long messageId, MessageStatus messageStatus, ContactId contactId) {

		System.out.println(String.format("%s: %s -> message #%d status: %s\n", myName,
				dmsHandle.getContactHandle(contactId).getName(), messageId, messageStatus));

	}

	@Override
	public void guiMessageSent(Long messageId, String message, ContactId contactId, GroupId groupId) {

		ContactHandle contact = dmsHandle.getContactHandle(contactId);
		GroupHandle group = dmsHandle.getGroupHandle(groupId);

		System.out.println(String.format("Message #%d sent to %s/%s: %s\n", messageId,
				contact == null ? null : contact.getName(), group == null ? null : group.getName(), message));

	}

	@Override
	public void guiMessageReceived(Long messageId, String message, ContactId contactId, GroupId groupId) {

		ContactHandle contact = dmsHandle.getContactHandle(contactId);
		GroupHandle group = dmsHandle.getGroupHandle(groupId);

		System.out.println(String.format("Message #%d received from %s/%s: %s\n", messageId,
				contact == null ? null : contact.getName(), group == null ? null : group.getName(), message));

//		if (groupId != null) {
//			dmsHandle.markGroupMessagesAsRead(groupId);
//		} else {
//			dmsHandle.markPrivateMessagesAsRead(contactId);
//		}

	}

	@Override
	public void guiFileSent(Long messageId, String message, Path path, ContactId contactId, GroupId groupId) {

		ContactHandle contact = dmsHandle.getContactHandle(contactId);
		GroupHandle group = dmsHandle.getGroupHandle(groupId);

		System.out.println(
				String.format("File #%d sent to %s/%s: %s, %s\n", messageId, contact == null ? null : contact.getName(),
						group == null ? null : group.getName(), path.getFileName().toString(), message));

	}

	@Override
	public void guiFileReceived(Long messageId, String message, Path path, ContactId contactId, GroupId groupId) {

		ContactHandle contact = dmsHandle.getContactHandle(contactId);
		GroupHandle group = dmsHandle.getGroupHandle(groupId);

		System.out.println(String.format("File #%d received from %s/%s: %s, %s\n", messageId,
				contact == null ? null : contact.getName(), group == null ? null : group.getName(),
				path.getFileName().toString(), message));

	}

	@Override
	public void guiReportSent(Long messageId, String message, Integer reportId, Path path, ContactId contactId,
			GroupId groupId) {

		ContactHandle contact = dmsHandle.getContactHandle(contactId);
		GroupHandle group = dmsHandle.getGroupHandle(groupId);

		System.out.println(String.format("Report-%d #%d sent to %s/%s: %s, %s\n", reportId, messageId,
				contact == null ? null : contact.getName(), group == null ? null : group.getName(),
				path.getFileName().toString(), message));

	}

	@Override
	public void guiReportReceived(Long messageId, String message, Integer reportId, Path path, ContactId contactId,
			GroupId groupId) {

		ContactHandle contact = dmsHandle.getContactHandle(contactId);
		GroupHandle group = dmsHandle.getGroupHandle(groupId);

		System.out.println(String.format("Report-%d #%d received from %s/%s: %s, %s\n", reportId, messageId,
				contact == null ? null : contact.getName(), group == null ? null : group.getName(),
				path.getFileName().toString(), message));

	}

	@Override
	public void guiAudioSent(Long messageId, Path path, ContactId contactId, GroupId groupId) {

		ContactHandle contact = dmsHandle.getContactHandle(contactId);
		GroupHandle group = dmsHandle.getGroupHandle(groupId);

		System.out.println(
				String.format("Audio #%d sent to %s/%s: %s\n", messageId, contact == null ? null : contact.getName(),
						group == null ? null : group.getName(), path.getFileName().toString()));

	}

	@Override
	public void guiAudioReceived(Long messageId, Path path, ContactId contactId, GroupId groupId) {

		ContactHandle contact = dmsHandle.getContactHandle(contactId);
		GroupHandle group = dmsHandle.getGroupHandle(groupId);

		System.out.println(String.format("Audio #%d received from %s/%s: %s\n", messageId,
				contact == null ? null : contact.getName(), group == null ? null : group.getName(),
				path.getFileName().toString()));

	}

	@Override
	public void guiMessagesRead(Long[] messageIds) {

		System.out
				.println(String.format("Okunan: %d, kalan: %d", messageIds.length, dmsHandle.getUnreadMessagesCount()));

	}

	@Override
	public void guiMessagesDeleted(Long[] messageIds) {

		System.out.println(String.format("Messages deleted: %s\n", Arrays.toString(messageIds)));

	}

	@Override
	public void guiPrivateConversationDeleted(ContactId contactId, Long[] deletedMessageIds) {

		System.out.println(String.format("Private conversation deleted: %s\nDeleted messages: %s\n",
				dmsHandle.getContactHandle(contactId).getName(), Arrays.toString(deletedMessageIds)));

	}

	@Override
	public void guiGroupConversationDeleted(GroupId groupId, Long[] deletedMessageIds) {

		System.out.println(String.format("Group conversation deleted: %s\nDeleted messages: %s\n",
				dmsHandle.getGroupHandle(groupId).getName(), Arrays.toString(deletedMessageIds)));

	}

	@Override
	public void guiPrivateConversationCleared(ContactId contactId, Long[] deletedMessageIds) {

		System.out.println(String.format("Private conversation cleared: %s\nDeleted messages: %s\n",
				dmsHandle.getContactHandle(contactId).getName(), Arrays.toString(deletedMessageIds)));

	}

	@Override
	public void guiGroupConversationCleared(GroupId groupId, Long[] deletedMessageIds) {

		System.out.println(String.format("Group conversation cleared: %s\nDeleted messages: %s\n",
				dmsHandle.getGroupHandle(groupId).getName(), Arrays.toString(deletedMessageIds)));

	}

	@Override
	public Path fileRequested(Long fileId) {
		System.out.println("FILE REQUESTED");
		return Paths.get("D:/Onur/E-kitap/JavaTM_ The Complete Reference, - Herbert Schildt.pdf");
	}

	@Override
	public void fileServerNotFound(Long downloadId) {
		System.out.println(String.format("File server not found [%d]", downloadId));
	}

	@Override
	public void fileNotFound(Long downloadId) {
		System.out.println(String.format("File not found [%d]", downloadId));
	}

	@Override
	public void downloadingFile(Long downloadId, int progress) {
		System.out.println(String.format("Downloading file [%d]: %d%%", downloadId, progress));
	}

	@Override
	public void fileDownloaded(Long downloadId, Path path) {
		System.out.println(String.format("File downloaded [%d]: %s", downloadId, path.toAbsolutePath().toString()));
	}

	@Override
	public void downloadPaused(Long downloadId) {
		System.out.println(String.format("Download paused [%d]", downloadId));
	}

	@Override
	public void downloadFailed(Long downloadId) {
		System.out.println(String.format("Download failed [%d]", downloadId));
	}

}
