package com.ogya.dms.test.main;

import java.io.IOException;
import java.nio.file.Path;

import com.ogya.dms.core.intf.DmsHandle;
import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.ListHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.intf.handles.ObjectHandle;
import com.ogya.dms.core.intf.listeners.DmsListener;

public class DmsListenerImpl implements DmsListener {

	private final DmsHandle dmsHandle;

	public DmsListenerImpl(DmsHandle dmsHandle) {

		this.dmsHandle = dmsHandle;

	}

	@Override
	public void fileClicked(Path path) {

		try {

			new ProcessBuilder().directory(path.getParent().toFile())
					.command("cmd", "/C", path.getFileName().toString()).start();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void messageReceived(MessageHandle messageHandle) {

		Long contactId = messageHandle.getContactId();
		Long groupId = messageHandle.getGroupId();

		System.out.println(String.format("Message received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getContactHandle(contactId).getName(),
				groupId == null ? null : dmsHandle.getGroupHandle(groupId).getName(), messageHandle.getMessage()));

	}

	@Override
	public void objectReceived(ObjectHandle objectHandle) {

		Long contactId = objectHandle.getContactId();
		Long groupId = objectHandle.getGroupId();

		System.out.println(String.format("Object received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getContactHandle(contactId).getName(),
				groupId == null ? null : dmsHandle.getGroupHandle(groupId).getName(),
				objectHandle.getObject(TestPojo.class)));

	}

	@Override
	public void listReceived(ListHandle listHandle) {

		Long contactId = listHandle.getContactId();
		Long groupId = listHandle.getGroupId();

		System.out.println(String.format("List received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getContactHandle(contactId).getName(),
				groupId == null ? null : dmsHandle.getGroupHandle(groupId).getName(),
				listHandle.getList(TestPojoConverted.class)));

	}

	@Override
	public void fileReceived(FileHandle fileHandle) {

		Long contactId = fileHandle.getContactId();
		Long groupId = fileHandle.getGroupId();

		System.out.println(String.format("File received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getContactHandle(contactId).getName(),
				groupId == null ? null : dmsHandle.getGroupHandle(groupId).getName(), fileHandle.getPath()));

		try {

			new ProcessBuilder().directory(fileHandle.getPath().getParent().toFile())
					.command("cmd", "/C", fileHandle.getPath().getFileName().toString()).start();

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

}
