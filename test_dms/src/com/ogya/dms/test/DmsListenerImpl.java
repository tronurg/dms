package com.ogya.dms.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

import com.ogya.dms.intf.DmsHandle;
import com.ogya.dms.intf.handles.ContactHandle;
import com.ogya.dms.intf.handles.FileHandle;
import com.ogya.dms.intf.handles.ListHandle;
import com.ogya.dms.intf.handles.MessageHandle;
import com.ogya.dms.intf.handles.ObjectHandle;
import com.ogya.dms.intf.listeners.DmsListener;

public class DmsListenerImpl implements DmsListener {

	private final DmsHandle dmsHandle;

	public DmsListenerImpl(DmsHandle dmsHandle) {

		this.dmsHandle = dmsHandle;

	}

	@Override
	public void fileClicked(Path file) {

		try {

			new ProcessBuilder().directory(file.getParent().toFile())
					.command("cmd", "/C", file.getFileName().toString()).start();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void messageReceived(MessageHandle messageHandle) {

		String senderUuid = messageHandle.getContactUuid();
		String groupUuid = messageHandle.getGroupUuid();

		System.out.println(String.format("Message received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getContactHandle(senderUuid).getName(),
				groupUuid == null ? null : dmsHandle.getGroupHandle(groupUuid).getName(), messageHandle.getMessage()));

	}

	@Override
	public void objectReceived(ObjectHandle objectHandle) {

		String senderUuid = objectHandle.getContactUuid();
		String groupUuid = objectHandle.getGroupUuid();

		System.out.println(String.format("Object received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getContactHandle(senderUuid).getName(),
				groupUuid == null ? null : dmsHandle.getGroupHandle(groupUuid).getName(),
				objectHandle.getObject(TestPojo.class)));

	}

	@Override
	public void listReceived(ListHandle listHandle) {

		String senderUuid = listHandle.getContactUuid();
		String groupUuid = listHandle.getGroupUuid();

		System.out.println(String.format("List received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getContactHandle(senderUuid).getName(),
				groupUuid == null ? null : dmsHandle.getGroupHandle(groupUuid).getName(),
				listHandle.getList(TestPojoConverted.class)));

	}

	@Override
	public void fileReceived(FileHandle fileHandle) {

		String senderUuid = fileHandle.getContactUuid();
		String groupUuid = fileHandle.getGroupUuid();

		System.out.println(String.format("File received from: %s (group: %s)\nContent: %s\n",
				dmsHandle.getContactHandle(senderUuid).getName(),
				groupUuid == null ? null : dmsHandle.getGroupHandle(groupUuid).getName(), fileHandle.getPath()));

		try {

			new ProcessBuilder().directory(fileHandle.getPath().getParent().toFile())
					.command("cmd", "/C", fileHandle.getPath().getFileName().toString()).start();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void contactUpdated(ContactHandle contactHandle) {

//		System.out.println(String.format("Contact updated: %s\n", contactHandle.getName()));

		System.out.println("I'm " + dmsHandle.getMyContactHandle().getName());
		try {
			dmsHandle.getUuidsByAddress(InetAddress.getByName("192.168.1.87"))
					.forEach(e -> System.out.println(dmsHandle.getContactHandle(e).getName()));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
