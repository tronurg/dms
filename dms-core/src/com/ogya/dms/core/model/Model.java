package com.ogya.dms.core.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.ViewStatus;

public class Model {

	private final Contact identity;

	private final String localUuid;

	private final AtomicBoolean isServerConnected = new AtomicBoolean(false);

	private final Queue<Runnable> serverTaskQueue = new ConcurrentLinkedQueue<Runnable>();

	private final Map<String, Contact> uuidContacts = Collections.synchronizedMap(new HashMap<String, Contact>());
	private final Map<Long, Contact> idContacts = Collections.synchronizedMap(new HashMap<Long, Contact>());
	private final Map<Long, Dgroup> idGroups = Collections.synchronizedMap(new HashMap<Long, Dgroup>());
	private final Map<String, Map<Long, Dgroup>> uuidRemoteGroups = Collections
			.synchronizedMap(new HashMap<String, Map<Long, Dgroup>>());

	private final Map<EntityId, Set<Message>> unreadMessages = Collections
			.synchronizedMap(new HashMap<EntityId, Set<Message>>());

	private final AtomicReference<EntityId> openEntityId = new AtomicReference<EntityId>();

	private final AtomicReference<Dgroup> groupToBeUpdated = new AtomicReference<Dgroup>();

	private final AtomicLong detailedGroupMessageId = new AtomicLong(-1L);

	private final AtomicLong minArchivedMessageId = new AtomicLong(-1L);

	private final Map<Long, Map<Long, Integer>> groupMessageProgresses = Collections
			.synchronizedMap(new HashMap<Long, Map<Long, Integer>>());

	private final Map<EntityId, Map<Long, Integer>> privateMessageProgresses = Collections
			.synchronizedMap(new HashMap<EntityId, Map<Long, Integer>>());

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

	public void updateSecretId(String secretId) {

		identity.setSecretId(secretId);

	}

	public boolean isServerConnected() {

		return isServerConnected.get();

	}

	public void setServerConnStatus(boolean connStatus) {

		isServerConnected.set(connStatus);

	}

	public void addServerTaskToQueue(Runnable task) {

		serverTaskQueue.offer(task);

	}

	public Runnable consumeServerTask() {

		return serverTaskQueue.poll();

	}

	public void addUpdateContact(Contact contact) {

		if (Objects.equals(contact.getViewStatus(), ViewStatus.DELETED)) {
			removeContact(contact);
			return;
		}

		uuidContacts.put(contact.getUuid(), contact);
		idContacts.put(contact.getId(), contact);

	}

	private void removeContact(Contact contact) {

		uuidContacts.remove(contact.getUuid());
		idContacts.remove(contact.getId());

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

	public boolean shouldCheckMessages(Contact newContact) {

		if (newContact == null)
			return false;

		Contact contact = uuidContacts.get(newContact.getUuid());

		if (contact == null || Objects.equals(contact.getLocalRemoteServerIps(), newContact.getLocalRemoteServerIps()))
			return false;

		if (!contact.getLocalRemoteServerIps().isEmpty()
				&& contact.getLocalRemoteServerIps().size() < newContact.getLocalRemoteServerIps().size())
			return false;

		return true;

	}

	public Map<Long, Contact> getContacts() {

		return idContacts;

	}

	public List<Long> getIdsByServerIp(InetAddress remoteServerIp) {

		if (remoteServerIp.isLoopbackAddress()) {
			try {
				remoteServerIp = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {

			}
		}

		final InetAddress ip = remoteServerIp;

		List<Long> ids = new ArrayList<Long>();

		idContacts.forEach((id, contact) -> {
			if (contact.getLocalRemoteServerIps().containsValue(ip))
				ids.add(id);
		});

		return ids;

	}

	public List<Long> getIdsByServerIpAndName(InetAddress remoteServerIp, final String name) {

		if (remoteServerIp.isLoopbackAddress()) {
			try {
				remoteServerIp = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {

			}
		}

		final InetAddress ip = remoteServerIp;

		List<Long> ids = new ArrayList<Long>();

		idContacts.forEach((id, contact) -> {
			if (contact.getLocalRemoteServerIps().containsValue(ip) && Objects.equals(contact.getName(), name))
				ids.add(id);
		});

		return ids;

	}

	public List<Long> getIdsByServerIpAndSecretId(InetAddress remoteServerIp, final String secretId) {

		if (remoteServerIp.isLoopbackAddress()) {
			try {
				remoteServerIp = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {

			}
		}

		final InetAddress ip = remoteServerIp;

		List<Long> ids = new ArrayList<Long>();

		idContacts.forEach((id, contact) -> {
			if (contact.getLocalRemoteServerIps().containsValue(ip) && Objects.equals(contact.getSecretId(), secretId))
				ids.add(id);
		});

		return ids;

	}

	public void addUpdateGroup(Dgroup group) {

		if (Objects.equals(group.getViewStatus(), ViewStatus.DELETED)) {
			removeGroup(group);
			return;
		}

		idGroups.put(group.getId(), group);

		if (group.isLocal())
			return;

		String ownerUuid = group.getOwner().getUuid();

		Map<Long, Dgroup> contactsGroups = uuidRemoteGroups.get(ownerUuid);
		if (contactsGroups == null) {
			contactsGroups = new HashMap<Long, Dgroup>();
			uuidRemoteGroups.put(ownerUuid, contactsGroups);
		}
		contactsGroups.put(group.getGroupRefId(), group);

	}

	private void removeGroup(Dgroup group) {

		idGroups.remove(group.getId());

		if (group.isLocal())
			return;

		String ownerUuid = group.getOwner().getUuid();

		Map<Long, Dgroup> contactsGroups = uuidRemoteGroups.get(ownerUuid);
		if (contactsGroups == null)
			return;
		contactsGroups.remove(group.getGroupRefId());
		if (contactsGroups.isEmpty())
			uuidRemoteGroups.remove(ownerUuid);

	}

	public Dgroup getGroup(Long groupId) {

		return idGroups.get(groupId);

	}

	public Dgroup getRemoteGroup(String ownerUuid, Long groupRefId) {

		Map<Long, Dgroup> contactsGroups = uuidRemoteGroups.get(ownerUuid);

		if (contactsGroups == null)
			return null;

		return contactsGroups.get(groupRefId);

	}

	public Map<Long, Dgroup> getGroups() {

		return idGroups;

	}

	public void registerMessage(Message message) {

		if (message.isLocal() || message.getUpdateType() != null)
			return;

		EntityId entityId = message.getEntity().getEntityId();

		Set<Message> unreadMessagesOfEntity = unreadMessages.get(entityId);

		if (Objects.equals(message.getMessageStatus(), MessageStatus.READ) && unreadMessagesOfEntity != null) {

			unreadMessagesOfEntity.remove(message);

			if (unreadMessagesOfEntity.isEmpty()) {
				unreadMessages.remove(entityId);
			}

		} else if (!Objects.equals(message.getMessageStatus(), MessageStatus.READ)) {

			if (unreadMessagesOfEntity == null) {
				unreadMessagesOfEntity = new HashSet<Message>();
				unreadMessages.put(entityId, unreadMessagesOfEntity);
			}

			unreadMessagesOfEntity.remove(message);
			unreadMessagesOfEntity.add(message);

		}

	}

	public void registerMessages(List<Message> messages) {

		messages.forEach(message -> registerMessage(message));

	}

	public Set<Message> getUnreadMessagesOfEntity(EntityId entityId) {

		return unreadMessages.get(entityId);

	}

	public void entityOpened(EntityId entityId) {

		openEntityId.set(entityId);

	}

	public void entityClosed() {

		openEntityId.set(null);

	}

	public EntityId getOpenEntityId() {

		return openEntityId.get();

	}

	public boolean isEntityOpen(EntityId entityId) {

		return Objects.equals(openEntityId.get(), entityId);

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

	public void setMinArchivedMessageId(long messageId) {

		minArchivedMessageId.set(messageId);

	}

	public Long getMinArchivedMessageId() {

		return minArchivedMessageId.get();

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

	public void storePrivateMessageProgress(EntityId entityId, Long messageId, int progress) {

		if (progress < 0 || progress == 100) {

			if (!privateMessageProgresses.containsKey(entityId))
				return;

			privateMessageProgresses.get(entityId).remove(messageId);

			if (privateMessageProgresses.get(entityId).isEmpty())
				privateMessageProgresses.remove(entityId);

		} else {

			privateMessageProgresses.putIfAbsent(entityId, Collections.synchronizedMap(new HashMap<Long, Integer>()));

			privateMessageProgresses.get(entityId).put(messageId, progress);

		}

	}

	public Map<EntityId, Map<Long, Integer>> getPrivateMessageProgresses() {

		return privateMessageProgresses;

	}

	public void clearPrivateMessageProgresses() {

		privateMessageProgresses.clear();

	}

}
