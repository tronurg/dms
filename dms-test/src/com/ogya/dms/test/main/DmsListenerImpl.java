package com.ogya.dms.test.main;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;

import com.ogya.dms.core.intf.DmsHandle;
import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.ListHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.intf.handles.ObjectHandle;
import com.ogya.dms.core.intf.listeners.DmsListener;
import com.ogya.dms.core.structures.MessageStatus;

public class DmsListenerImpl implements DmsListener {

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

//		System.out.println(String.format("Kisi guncellendi: %s\n", contactHandle.getName()));

	}

	@Override
	public void groupUpdated(GroupHandle groupHandle) {

//		System.out.println(String.format("Grup guncellendi: %s\n", groupHandle.getName()));

	}

	@Override
	public void messageReceived(MessageHandle messageHandle, Long contactId, Long groupId) {

		String groupName = groupId == null ? null : dmsHandle.getGroupHandle(groupId).getName();

		System.out.println(String.format("%s: Message received from: %s (group: %s)\nContent: %s\n", myName,
				dmsHandle.getContactHandle(contactId).getName(), groupName, messageHandle.getMessage()));

		FileHandle fileHandle = messageHandle.getFileHandle();
		if (fileHandle != null) {
			System.out.println(String.format("%s: File received from: %s (group: %s)\nContent: %s\n", myName,
					dmsHandle.getContactHandle(contactId).getName(), groupName, fileHandle.getPath()));
			try {
				new ProcessBuilder().directory(fileHandle.getPath().getParent().toFile())
						.command("cmd", "/C", fileHandle.getPath().getFileName().toString()).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		ObjectHandle objectHandle = messageHandle.getObjectHandle();
		if (objectHandle != null) {
			System.out.println(String.format("%s: Object received from: %s (group: %s)\nContent: %s\n", myName,
					dmsHandle.getContactHandle(contactId).getName(), groupName,
					objectHandle.getObject(TestPojo.class)));
		}

		ListHandle listHandle = messageHandle.getListHandle();
		if (listHandle != null) {
			System.out.println(String.format("%s: List received from: %s (group: %s)\nContent: %s\n", myName,
					dmsHandle.getContactHandle(contactId).getName(), groupName,
					listHandle.getList(TestPojoConverted.class)));
		}

		try {
			dmsHandle.sendMessageToContacts(messageHandle,
					dmsHandle.getIdsByAddressAndName(InetAddress.getByName("192.168.1.88"), "elma"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void messageTransmitted(Integer trackingId, Long contactId) {

		System.out.println(String.format("%s: Message #%d transmitted to %s\n", myName, trackingId,
				dmsHandle.getContactHandle(contactId).getName()));

	}

	@Override
	public void guiMessageStatusUpdated(Long messageId, MessageStatus messageStatus, Long contactId) {

		System.out.println(String.format("%s: %s -> message #%d status: %s\n", myName,
				dmsHandle.getContactHandle(contactId).getName(), messageId, messageStatus));

	}

}
