package com.ogya.dms.model;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.structures.Availability;

public class Model {

	private final Contact identity;

	private final String localUuid;

	private final AtomicBoolean isServerConnected = new AtomicBoolean(false);

	private final Map<String, Contact> uuidContacts = Collections.synchronizedMap(new HashMap<String, Contact>());
	private final Map<Long, Contact> idContacts = Collections.synchronizedMap(new HashMap<Long, Contact>());
	private final Map<String, Map<Long, Dgroup>> uuidGroups = Collections
			.synchronizedMap(new HashMap<String, Map<Long, Dgroup>>());
	private final Map<Long, Dgroup> idGroups = Collections.synchronizedMap(new HashMap<Long, Dgroup>());

	private final Map<Long, List<InetAddress>> idAddresses = Collections
			.synchronizedMap(new HashMap<Long, List<InetAddress>>());

	private final List<Long> openUuids = Collections.synchronizedList(new ArrayList<Long>());

	private final AtomicReference<Dgroup> groupToBeUpdated = new AtomicReference<Dgroup>();

	private final AtomicLong detailedGroupMessageId = new AtomicLong(-1L);

	private final AtomicReference<Long> referenceId = new AtomicReference<Long>();

	private final Map<Long, Map<Long, Integer>> groupMessageProgresses = Collections
			.synchronizedMap(new HashMap<Long, Map<Long, Integer>>());

	private final Map<Long, Map<Long, Integer>> privateMessageProgresses = Collections
			.synchronizedMap(new HashMap<Long, Map<Long, Integer>>());

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

	public Model(Contact identity) {

		this.identity = identity;

		this.localUuid = identity.getUuid();

	}

	public Contact getIdentity() {

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

		uuidContacts.put(contact.getUuid(), contact);
		idContacts.put(contact.getId(), contact);

	}

	public Contact getContact(String uuid) {

		return uuidContacts.get(uuid);

	}

	public Contact getContact(Long id) {

		return idContacts.get(id);

	}

	public boolean isContactOnline(String uuid) {

		return uuidContacts.containsKey(uuid) && !Objects.equals(getContact(uuid).getStatus(), Availability.OFFLINE);

	}

	public Map<Long, Contact> getContacts() {

		return idContacts;

	}

	public void setContactAddresses(Long id, List<InetAddress> addresses) {

		if (addresses == null)
			idAddresses.remove(id);
		else
			idAddresses.put(id, addresses);

	}

	public List<Long> getIdsByAddress(InetAddress address) {

		try {

			if (NetworkInterface.getByInetAddress(address) != null)
				address = InetAddress.getByName("localhost");

		} catch (SocketException | UnknownHostException e) {

			e.printStackTrace();

		}

		final InetAddress searchAddress = address;

		List<Long> ids = new ArrayList<Long>();

		idAddresses.entrySet().stream().filter(entry -> entry.getValue().contains(searchAddress))
				.forEach(entry -> ids.add(entry.getKey()));

		return ids;

	}

	public void addGroup(Dgroup group) {

		idGroups.put(group.getId(), group);

		if (Objects.equals(group.getOwner().getUuid(), localUuid))
			return;

		String ownerUuid = group.getOwner().getUuid();

		uuidGroups.putIfAbsent(ownerUuid, new HashMap<Long, Dgroup>());
		uuidGroups.get(ownerUuid).put(group.getGroupRefId(), group);

	}

	public Dgroup getGroup(Long groupId) {

		if (groupId == null)
			return null;

		return idGroups.get(groupId);

	}

	public Dgroup getGroup(String ownerUuid, Long groupRefId) {

		if (uuidGroups.containsKey(ownerUuid))
			return uuidGroups.get(ownerUuid).get(groupRefId);

		return null;

	}

	public Map<Long, Dgroup> getGroups() {

		return idGroups;

	}

	public void messagePaneOpened(Long id) {

		openUuids.add(id);

	}

	public void messagePaneClosed(Long id) {

		openUuids.remove(id);

	}

	public boolean isMessagePaneOpen(Long id) {

		return openUuids.contains(id);

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

	public void setReferenceId(Long id) {

		referenceId.set(id);

	}

	public Long getReferenceId() {

		return referenceId.get();

	}

	public Comparator<Contact> getContactSorter() {

		return contactSorter;

	}

	public Comparator<String> getCaseInsensitiveStringSorter() {

		return caseInsensitiveStringSorter;

	}

	public void storeGroupMessageProgress(Long messageId, Long contactId, int progress) {

		if (progress < 0 || progress == 100) {

			if (!groupMessageProgresses.containsKey(messageId))
				return;

			groupMessageProgresses.get(messageId).remove(contactId);

			if (groupMessageProgresses.get(messageId).isEmpty())
				groupMessageProgresses.remove(messageId);

		} else {

			groupMessageProgresses.putIfAbsent(messageId, Collections.synchronizedMap(new HashMap<Long, Integer>()));

			groupMessageProgresses.get(messageId).put(contactId, progress);

		}

	}

	public Map<Long, Integer> getGroupMessageProgresses(Long messageId) {

		return groupMessageProgresses.get(messageId);

	}

	public void clearGroupMessageProgresses() {

		groupMessageProgresses.clear();

	}

	public void storePrivateMessageProgress(Long contactId, Long messageId, int progress) {

		if (progress < 0 || progress == 100) {

			if (!privateMessageProgresses.containsKey(contactId))
				return;

			privateMessageProgresses.get(contactId).remove(messageId);

			if (privateMessageProgresses.get(contactId).isEmpty())
				privateMessageProgresses.remove(contactId);

		} else {

			privateMessageProgresses.putIfAbsent(contactId, Collections.synchronizedMap(new HashMap<Long, Integer>()));

			privateMessageProgresses.get(contactId).put(messageId, progress);

		}

	}

	public Map<Long, Map<Long, Integer>> getPrivateMessageProgresses() {

		return privateMessageProgresses;

	}

	public void clearPrivateMessageProgresses() {

		privateMessageProgresses.clear();

	}

}
