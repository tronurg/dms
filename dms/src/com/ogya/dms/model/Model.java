package com.ogya.dms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.database.tables.Identity;
import com.ogya.dms.structures.Availability;
import com.ogya.dms.structures.ReceiverType;

public class Model {

	private final Identity identity;

	private final String localUuid;

	private final AtomicBoolean isServerConnected = new AtomicBoolean(false);

	private final Map<String, Contact> contacts = Collections.synchronizedMap(new HashMap<String, Contact>());
	private final Map<String, Dgroup> groups = Collections.synchronizedMap(new HashMap<String, Dgroup>());

	private final List<String> openUuids = Collections.synchronizedList(new ArrayList<String>());

	private final Map<String, Long> minMessageIds = Collections.synchronizedMap(new HashMap<String, Long>());

	private final AtomicReference<Dgroup> groupToBeUpdated = new AtomicReference<Dgroup>();

	private final AtomicLong detailedGroupMessageId = new AtomicLong();

	private final AtomicReference<Entry<ReceiverType, String>> fileSelectionUuid = new AtomicReference<Entry<ReceiverType, String>>();

	private final AtomicReference<Entry<ReceiverType, String>> recordUuid = new AtomicReference<Entry<ReceiverType, String>>();

	private final Map<Long, Map<String, Integer>> groupMessageProgresses = Collections
			.synchronizedMap(new HashMap<Long, Map<String, Integer>>());

	private final Map<String, Map<Long, Integer>> privateMessageProgresses = Collections
			.synchronizedMap(new HashMap<String, Map<Long, Integer>>());

	private final Comparator<Contact> contactSorter = new Comparator<Contact>() {
		@Override
		public int compare(Contact arg0, Contact arg1) {
			return arg0.getName().toLowerCase().compareTo(arg1.getName().toLowerCase());
		}
	};

	private final Comparator<String> caseInsensitiveStringSorter = new Comparator<String>() {
		@Override
		public int compare(String arg0, String arg1) {
			return arg0.toLowerCase().compareTo(arg1.toLowerCase());
		}
	};

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

	public void updateStatus(Availability status) {

		identity.setStatus(status);

	}

	public void updateCoordinates(Double lattitude, Double longitude) {

		identity.setLattitude(lattitude);
		identity.setLongitude(longitude);

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

		return contacts.containsKey(uuid) && !Objects.equals(getContact(uuid).getStatus(), Availability.OFFLINE);

	}

	public Map<String, Contact> getContacts() {

		return contacts;

	}

	public void addGroup(Dgroup group) {

		groups.put(group.getUuid(), group);

	}

	public Dgroup getGroup(String uuid) {

		if (uuid == null)
			return null;

		return groups.get(uuid);

	}

	public Map<String, Dgroup> getGroups() {

		return groups;

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

	public void setGroupToBeUpdated(Dgroup group) {

		groupToBeUpdated.set(group);

	}

	public Dgroup getGroupToBeUpdated() {

		return groupToBeUpdated.get();

	}

	public void setDetailedGroupMessageId(long messageId) {

		detailedGroupMessageId.set(messageId);

	}

	public Long getDetailedGroupMessageId() {

		return detailedGroupMessageId.get();

	}

	public void setFileSelectionUuid(Entry<ReceiverType, String> entry) {

		fileSelectionUuid.set(entry);

	}

	public Entry<ReceiverType, String> getFileSelectionUuid() {

		return fileSelectionUuid.get();

	}

	public void setRecordUuid(Entry<ReceiverType, String> entry) {

		recordUuid.set(entry);

	}

	public Entry<ReceiverType, String> getRecordUuid() {

		return recordUuid.get();

	}

	public Comparator<Contact> getContactSorter() {

		return contactSorter;

	}

	public Comparator<String> getCaseInsensitiveStringSorter() {

		return caseInsensitiveStringSorter;

	}

	public void storeGroupMessageProgress(Long messageId, String uuid, int progress) {

		if (progress < 0 || progress == 100) {

			if (!groupMessageProgresses.containsKey(messageId))
				return;

			groupMessageProgresses.get(messageId).remove(uuid);

			if (groupMessageProgresses.get(messageId).isEmpty())
				groupMessageProgresses.remove(messageId);

		} else {

			groupMessageProgresses.putIfAbsent(messageId, Collections.synchronizedMap(new HashMap<String, Integer>()));

			groupMessageProgresses.get(messageId).put(uuid, progress);

		}

	}

	public Map<String, Integer> getGroupMessageProgresses(Long messageId) {

		return groupMessageProgresses.get(messageId);

	}

	public void clearGroupMessageProgresses() {

		groupMessageProgresses.clear();

	}

	public void storePrivateMessageProgress(String uuid, Long messageId, int progress) {

		if (progress < 0 || progress == 100) {

			if (!privateMessageProgresses.containsKey(uuid))
				return;

			privateMessageProgresses.get(uuid).remove(messageId);

			if (privateMessageProgresses.get(uuid).isEmpty())
				privateMessageProgresses.remove(uuid);

		} else {

			privateMessageProgresses.putIfAbsent(uuid, Collections.synchronizedMap(new HashMap<Long, Integer>()));

			privateMessageProgresses.get(uuid).put(messageId, progress);

		}

	}

	public Map<String, Map<Long, Integer>> getPrivateMessageProgresses() {

		return privateMessageProgresses;

	}

	public void clearPrivateMessageProgresses() {

		privateMessageProgresses.clear();

	}

}
