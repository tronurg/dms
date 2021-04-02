package com.ogya.dms.core.database;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import com.ogya.dms.core.common.CommonConstants;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.database.tables.Member;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.database.tables.StatusReport;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.ViewStatus;

public class DbManager {

	private final String name;

	private final SessionFactory factory;

	public DbManager(String dbName, String dbPassword) throws Exception {

		name = dbName;

		if (name.length() > 40)
			throw new Exception("Name too long, cannot exceed 40 characters.");

		factory = new Configuration().configure("/resources/hibernate.cfg/dms.cfg.xml")
				.setProperty("hibernate.connection.url",
						"jdbc:h2:split:24:" + CommonConstants.DB_PATH + File.separator + dbName)
				.setProperty("hibernate.connection.username", dbName)
				.setProperty("hibernate.connection.password", dbPassword).addAnnotatedClass(Contact.class)
				.addAnnotatedClass(Dgroup.class).addAnnotatedClass(Member.class).addAnnotatedClass(Message.class)
				.addAnnotatedClass(StatusReport.class).buildSessionFactory();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> factory.close()));

	}

	public Contact getIdentity() throws HibernateException {

		Session session = factory.openSession();

		Contact identity = session.createQuery("from Contact", Contact.class).setMaxResults(1).uniqueResult();

		if (identity == null) {

			identity = new Contact(UUID.randomUUID().toString());

			identity.setName(name);

			identity.setStatus(Availability.AVAILABLE);

			session.beginTransaction();

			session.persist(identity);

			session.getTransaction().commit();

		}

		session.close();

		return identity;

	}

	public List<Contact> fetchAllContacts() throws HibernateException {

		Session session = factory.openSession();

		Query<Contact> queryContact = session.createQuery("from Contact", Contact.class);

		List<Contact> allContacts = queryContact.list();

		allContacts = allContacts.subList(1, allContacts.size());

		session.close();

		return allContacts;

	}

	public List<Dgroup> fetchAllGroups() throws HibernateException {

		Session session = factory.openSession();

		Query<Dgroup> queryGroup = session.createQuery("from Dgroup", Dgroup.class);

		List<Dgroup> allGroups = queryGroup.list();

		session.close();

		return allGroups;

	}

	public Contact updateIdentity(Contact identity) throws HibernateException {

		Session session = factory.openSession();

		session.beginTransaction();

		Contact newIdentity = (Contact) session.merge(identity);

		session.getTransaction().commit();

		session.close();

		return newIdentity;

	}

	public Contact addUpdateContact(Contact contact) throws HibernateException {

		Session session = factory.openSession();

		Contact dbContact = session.createQuery("from Contact where uuid like :uuid", Contact.class)
				.setParameter("uuid", contact.getUuid()).uniqueResult();

		if (dbContact == null) {

			dbContact = contact;

			dbContact.setId(null);

			session.beginTransaction();

			session.persist(dbContact);

			session.getTransaction().commit();

		} else {

			contact.setId(dbContact.getId());

			session.beginTransaction();

			dbContact = (Contact) session.merge(contact);

			session.getTransaction().commit();

		}

		session.close();

		return dbContact;

	}

	public Contact getContact(String uuid) throws HibernateException {

		Session session = factory.openSession();

		Contact dbContact = session.createQuery("from Contact where uuid like :uuid", Contact.class)
				.setParameter("uuid", uuid).uniqueResult();

		session.close();

		return dbContact;

	}

	public Dgroup addUpdateGroup(Dgroup group) throws HibernateException {

		Session session = factory.openSession();

		Dgroup dbGroup = session
				.createQuery("from Dgroup where owner.uuid like :ownerUuid and groupRefId=:groupRefId", Dgroup.class)
				.setParameter("ownerUuid", group.getOwner().getUuid()).setParameter("groupRefId", group.getGroupRefId())
				.uniqueResult();

		if (dbGroup == null) {

			dbGroup = group;

			dbGroup.setId(null);

			session.beginTransaction();

			session.persist(dbGroup);

			session.getTransaction().commit();

		} else {

			group.setId(dbGroup.getId());

			session.beginTransaction();

			dbGroup = (Dgroup) session.merge(group);

			session.getTransaction().commit();

		}

		session.close();

		return dbGroup;

	}

	public Member addUpdateMember(Member member) throws HibernateException {

		Session session = factory.openSession();

		Member dbMember = session
				.createQuery("from Member where owner=:owner and contactRefId=:contactRefId", Member.class)
				.setParameter("owner", member.getOwner()).setParameter("contactRefId", member.getContactRefId())
				.uniqueResult();

		if (dbMember == null) {

			dbMember = member;

			dbMember.setId(null);

			session.beginTransaction();

			session.persist(dbMember);

			session.getTransaction().commit();

		} else {

			member.setId(dbMember.getId());

			session.beginTransaction();

			dbMember = (Member) session.merge(member);

			session.getTransaction().commit();

		}

		session.close();

		return dbMember;

	}

	public Member getMember(String ownerUuid, Long contactRefId) throws HibernateException {

		if (ownerUuid == null || contactRefId == null)
			return null;

		Session session = factory.openSession();

		Member dbMember = session
				.createQuery("from Member where owner.uuid like :ownerUuid and contactRefId=:contactRefId",
						Member.class)
				.setParameter("ownerUuid", ownerUuid).setParameter("contactRefId", contactRefId).uniqueResult();

		session.close();

		return dbMember;

	}

	public Message addUpdateMessage(Message message) throws HibernateException {

		Session session = factory.openSession();

		if (message.getId() == null) {

			resolveReferenceOfMessage(message, session);

			session.beginTransaction();

			session.persist(message);

			session.getTransaction().commit();

		} else {

			session.beginTransaction();

			message = (Message) session.merge(message);

			session.getTransaction().commit();

		}

		session.close();

		return message;

	}

	public List<Message> addUpdateMessages(List<Message> messages) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = new ArrayList<Message>();

		messages.forEach(message -> {

			if (message.getId() == null) {

				resolveReferenceOfMessage(message, session);

				session.beginTransaction();

				session.persist(message);

				session.getTransaction().commit();

			} else {

				session.beginTransaction();

				message = (Message) session.merge(message);

				session.getTransaction().commit();

			}

			dbMessages.add(message);

		});

		session.close();

		return dbMessages;

	}

	public Message getMessageBySender(String contactUuid, long messageRefId) throws HibernateException {

		Session session = factory.openSession();

		Message dbMessage = session.createQuery(
				"from Message where contact.uuid like :contactUuid and messageRefId=:messageRefId and local=false",
				Message.class).setParameter("contactUuid", contactUuid).setParameter("messageRefId", messageRefId)
				.uniqueResult();

		session.close();

		return dbMessage;

	}

	public Message getMessageById(Long id) throws HibernateException {

		if (id == null)
			return null;

		Session session = factory.openSession();

		Message dbMessage = session.createQuery("from Message where id=:id", Message.class).setParameter("id", id)
				.uniqueResult();

		session.close();

		return dbMessage;

	}

	public List<Message> getMessagesById(Long... ids) throws HibernateException {

		if (ids == null)
			return null;

		Session session = factory.openSession();

		List<Message> dbMessages = new ArrayList<Message>();

		for (Long id : ids) {

			if (id == null)
				continue;

			Message dbMessage = session.createQuery("from Message where id=:id", Message.class).setParameter("id", id)
					.uniqueResult();

			dbMessages.add(dbMessage);

		}

		session.close();

		return dbMessages;

	}

	public List<Message> getPrivateMessagesWaitingToContact(Long contactId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where done=false and contact.id=:contactId and local=true and dgroup is null",
				Message.class).setParameter("contactId", contactId).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getPrivateMessagesWaitingFromContact(Long contactId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where contact.id=:contactId and local=false and dgroup is null and messageStatus not like :read and updateType is null",
				Message.class).setParameter("contactId", contactId).setParameter("read", MessageStatus.READ).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getAllPrivateMessagesSinceFirstUnreadMessage(Long contactId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbFirstUnreadMessage = session.createQuery(
				"from Message where viewStatus not like :deleted and contact.id=:contactId and local=false and dgroup is null and messageStatus not like :read and updateType is null",
				Message.class).setParameter("deleted", ViewStatus.DELETED).setParameter("contactId", contactId)
				.setParameter("read", MessageStatus.READ).setMaxResults(1).list();

		if (dbFirstUnreadMessage.size() == 0) {

			return Collections.emptyList();

		}

		Long firstId = dbFirstUnreadMessage.get(0).getId();

		List<Message> dbMessages = session.createQuery(
				"from Message where viewStatus not like :deleted and id>=:firstId and contact.id=:contactId and dgroup is null and updateType is null",
				Message.class).setParameter("deleted", ViewStatus.DELETED).setParameter("firstId", firstId)
				.setParameter("contactId", contactId).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastPrivateMessages(Long contactId, int messageCount) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where viewStatus not like :deleted and contact.id=:contactId and dgroup is null and updateType is null order by id desc",
				Message.class).setParameter("deleted", ViewStatus.DELETED).setParameter("contactId", contactId)
				.setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastPrivateMessagesBeforeId(Long contactId, long messageId, int messageCount)
			throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where viewStatus not like :deleted and id<:messageId and contact.id=:contactId and dgroup is null and updateType is null order by id desc",
				Message.class).setParameter("deleted", ViewStatus.DELETED).setParameter("messageId", messageId)
				.setParameter("contactId", contactId).setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getGroupMessagesWaitingToContact(Long contactId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"select m from Message m join m.statusReports s where m.done=false and s.contactId=:contactId and s.messageStatus not like :read and (m.dgroup.local=true or m.dgroup.owner.id=:contactId)",
				Message.class).setParameter("contactId", contactId).setParameter("read", MessageStatus.READ).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getGroupMessagesWaitingToItsGroup(Long contactId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"select m from Message m join m.statusReports s where m.done=false and s.contactId=:contactId and s.messageStatus not like :fresh and m.dgroup.owner.id=:contactId",
				Message.class).setParameter("contactId", contactId).setParameter("fresh", MessageStatus.FRESH).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getMessagesWaitingFromGroup(Long groupId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where dgroup.id=:groupId and local=false and messageStatus not like :read and updateType is null",
				Message.class).setParameter("groupId", groupId).setParameter("read", MessageStatus.READ).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getAllGroupMessagesSinceFirstUnreadMessage(Long groupId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbFirstUnreadMessage = session.createQuery(
				"from Message where viewStatus not like :deleted and dgroup.id=:groupId and local=false and messageStatus not like :read and updateType is null",
				Message.class).setParameter("deleted", ViewStatus.DELETED).setParameter("groupId", groupId)
				.setParameter("read", MessageStatus.READ).setMaxResults(1).list();

		if (dbFirstUnreadMessage.size() == 0) {

			return Collections.emptyList();

		}

		Long firstId = dbFirstUnreadMessage.get(0).getId();

		List<Message> dbMessages = session.createQuery(
				"from Message where viewStatus not like :deleted and id>=:firstId and dgroup.id=:groupId and updateType is null",
				Message.class).setParameter("deleted", ViewStatus.DELETED).setParameter("firstId", firstId)
				.setParameter("groupId", groupId).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastGroupMessages(Long groupId, int messageCount) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where viewStatus not like :deleted and dgroup.id=:groupId and updateType is null order by id desc",
				Message.class).setParameter("deleted", ViewStatus.DELETED).setParameter("groupId", groupId)
				.setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastGroupMessagesBeforeId(Long groupId, long messageId, int messageCount)
			throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where viewStatus not like :deleted and id<:messageId and dgroup.id=:groupId and updateType is null order by id desc",
				Message.class).setParameter("deleted", ViewStatus.DELETED).setParameter("messageId", messageId)
				.setParameter("groupId", groupId).setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	public List<Dgroup> getAllActiveGroupsOfContact(Long contactId) throws HibernateException {

		Session session = factory.openSession();

		List<Dgroup> dbGroups = session
				.createQuery("from Dgroup where owner.id=:contactId and active=true", Dgroup.class)
				.setParameter("contactId", contactId).list();

		session.close();

		return dbGroups;

	}

	public List<Message> getLastArchivedMessages(int messageCount) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session
				.createQuery("from Message where viewStatus like :archived and updateType is null order by id desc",
						Message.class)
				.setParameter("archived", ViewStatus.ARCHIVED).setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastArchivedMessagesBeforeId(long messageId, int messageCount) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where viewStatus like :archived and id<:messageId and updateType is null order by id desc",
				Message.class).setParameter("archived", ViewStatus.ARCHIVED).setParameter("messageId", messageId)
				.setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	private void resolveReferenceOfMessage(Message message, Session session) throws HibernateException {

		Message refMessage = message.getRefMessage();

		if (refMessage == null || message.isLocal())
			return;

		Message dbMessage = null;

		try {

			if (refMessage.getMessageRefId() == null) {

				dbMessage = session
						.createQuery("from Message where messageRefId=:messageRefId and contact=:contact",
								Message.class)
						.setParameter("messageRefId", refMessage.getId()).setParameter("contact", message.getContact())
						.uniqueResult();

			} else if (message.getDgroup() == null || message.getDgroup().isLocal()) {

				dbMessage = session.createQuery("from Message where id=:id", Message.class)
						.setParameter("id", refMessage.getMessageRefId()).uniqueResult();

			} else {

				dbMessage = session
						.createQuery("from Message where messageRefId=:messageRefId and contact=:contact",
								Message.class)
						.setParameter("messageRefId", refMessage.getId()).setParameter("contact", message.getContact())
						.uniqueResult();

				if (dbMessage == null) {

					dbMessage = session.createQuery("from Message where id=:id and contact=:contact", Message.class)
							.setParameter("id", refMessage.getMessageRefId())
							.setParameter("contact", message.getContact()).uniqueResult();

				}

			}

		} catch (Exception e) {

		}

		message.setRefMessage(dbMessage);

	}

}
