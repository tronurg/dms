package com.ogya.dms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.database.tables.Identity;
import com.ogya.dms.structures.ContactStatus;

public class Model {

	private final Identity identity;

	private final String localUuid;

	private final AtomicBoolean isServerConnected = new AtomicBoolean(false);

	private final Map<String, Contact> contacts = Collections.synchronizedMap(new HashMap<String, Contact>());
	private final Map<String, Dgroup> groups = Collections.synchronizedMap(new HashMap<String, Dgroup>());

	private final List<String> openUuids = Collections.synchronizedList(new ArrayList<String>());

	private final Map<String, Long> minMessageIds = Collections.synchronizedMap(new HashMap<String, Long>());

	public Model(Identity identity) {

		this.identity = identity;

		this.localUuid = identity.getUuid();

	}

	public Identity getIdentity() {

		return identity;

	}

	public String getLocalUuid() {

		return localUuid;

	}

	public void updateComment(String comment) {

		identity.setComment(comment);

	}

	public void updateStatus(ContactStatus status) {

		identity.setStatus(status);

	}

	public boolean isServerConnected() {

		return isServerConnected.get();

	}

	public void setServerConnStatus(boolean connStatus) {

		isServerConnected.set(connStatus);

	}

	public void addContact(Contact contact) {

		contacts.put(contact.getUuid(), contact);

	}

	public Contact getContact(String uuid) {

		return contacts.get(uuid);

	}

	public boolean isContactOnline(String uuid) {

		return contacts.containsKey(uuid) && !getContact(uuid).getStatus().equals(ContactStatus.OFFLINE);

	}

	public Map<String, Contact> getContacts() {

		return contacts;

	}

	public void addGroup(Dgroup group) {

		groups.put(group.getUuid(), group);

	}

	public Dgroup getGroup(String uuid) {

		return groups.get(uuid);

	}

	public Map<String, Dgroup> getGroups() {

		return groups;

	}

	public boolean isMyGroup(String groupUuid) {

		return groups.containsKey(groupUuid) && localUuid.equals(groups.get(groupUuid).getUuidOwner());

	}

	public void messagePaneOpened(String uuid) {

		openUuids.add(uuid);

	}

	public void messagePaneClosed(String uuid) {

		openUuids.remove(uuid);

	}

	public boolean isMessagePaneOpen(String uuid) {

		return openUuids.contains(uuid);

	}

	public void addMessageId(String uuid, Long id) {

		if (minMessageIds.containsKey(uuid) && minMessageIds.get(uuid) < id)
			return;

		minMessageIds.put(uuid, id);

	}

	public Long getMinMessageId(String uuid) {

		if (!minMessageIds.containsKey(uuid))
			return -1L;

		return minMessageIds.get(uuid);

	}

}
