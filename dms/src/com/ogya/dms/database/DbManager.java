package com.ogya.dms.database;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import com.ogya.dms.common.CommonConstants;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.intf.exceptions.DbException;
import com.ogya.dms.structures.Availability;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageType;
import com.ogya.dms.structures.ReceiverType;
import com.ogya.dms.structures.WaitStatus;

public class DbManager {

	private final String name;

	private final SessionFactory factory;

	public DbManager(String dbName, String dbPassword) throws DbException {

		name = dbName;

		try {

			factory = new Configuration().configure(new File("./plugins/dms/hibernate.cfg/dms.cfg.xml"))
					.setProperty("hibernate.connection.url",
							"jdbc:h2:" + CommonConstants.DB_PATH + File.separator + dbName)
					.setProperty("hibernate.connection.username", dbName)
					.setProperty("hibernate.connection.password", dbPassword).addAnnotatedClass(Dgroup.class)
					.addAnnotatedClass(Contact.class).addAnnotatedClass(Message.class).buildSessionFactory();

			Runtime.getRuntime().addShutdownHook(new Thread(() -> factory.close()));

		} catch (HibernateException e) {

			e.printStackTrace();

			throw new DbException("Database cannot be accessed. Wrong password or account in use.");

		}

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

	public List<Message> fetchAllMessages() throws HibernateException {

		Session session = factory.openSession();

		Query<Message> queryMessage = session.createQuery("from Message", Message.class);

		List<Message> allMessages = queryMessage.list();

		session.close();

		return allMessages;

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

	public Message addUpdateMessage(Message message) throws HibernateException {

		Session session = factory.openSession();

		Message dbMessage = session.createQuery(
				"from Message where contact.uuid like :contactUuid and messageRefId=:messageRefId and messageDirection like :messageDirection",
				Message.class).setParameter("contactUuid", message.getContact().getUuid())
				.setParameter("messageRefId", message.getMessageRefId())
				.setParameter("messageDirection", message.getMessageDirection()).uniqueResult();

		if (dbMessage == null) {

			dbMessage = message;

			dbMessage.setId(null);

			session.beginTransaction();

			session.persist(dbMessage);

			session.getTransaction().commit();

		} else {

			message.setId(dbMessage.getId());

			session.beginTransaction();

			dbMessage = (Message) session.merge(message);

			session.getTransaction().commit();

		}

		session.close();

		return dbMessage;

	}

	public Message getMessage(String contactUuid, long messageRefId) throws HibernateException {

		Session session = factory.openSession();

		Message dbMessage = session
				.createQuery("from Message where contact.uuid like :contactUuid and messageRefId=:messageRefId",
						Message.class)
				.setParameter("contactUuid", contactUuid).setParameter("messageRefId", messageRefId).uniqueResult();

		session.close();

		return dbMessage;

	}

	public Message getMessage(long id) throws HibernateException {

		Session session = factory.openSession();

		Message dbMessage = session.createQuery("from Message where id=:id", Message.class).setParameter("id", id)
				.uniqueResult();

		session.close();

		return dbMessage;

	}

	public List<Message> getPrivateMessagesWaitingToContact(Long contactId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where contact.id=:contactId and messageDirection like :out and receiverType like :contact and waitStatus like :waiting",
				Message.class).setParameter("contactId", contactId).setParameter("out", MessageDirection.OUT)
				.setParameter("contact", ReceiverType.CONTACT).setParameter("waiting", WaitStatus.WAITING).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getPrivateMessagesWaitingFromContact(Long contactId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where contact.id=:contactId and messageDirection like :in and receiverType like :contact and messageStatus not like :read and messageType not like :update",
				Message.class).setParameter("contactId", contactId).setParameter("in", MessageDirection.IN)
				.setParameter("contact", ReceiverType.CONTACT).setParameter("read", MessageStatus.READ)
				.setParameter("update", MessageType.UPDATE).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getAllPrivateMessagesSinceFirstUnreadMessage(Long contactId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbFirstUnreadMessage = session.createQuery(
				"from Message where contact.id=:contactId and messageDirection like :in and receiverType like :contact and messageStatus not like :read and messageType not like :update",
				Message.class).setParameter("contactId", contactId).setParameter("in", MessageDirection.IN)
				.setParameter("contact", ReceiverType.CONTACT).setParameter("read", MessageStatus.READ)
				.setParameter("update", MessageType.UPDATE).setMaxResults(1).list();

		if (dbFirstUnreadMessage.size() == 0) {

			return Collections.emptyList();

		}

		Long firstId = dbFirstUnreadMessage.get(0).getId();

		List<Message> dbMessages = session.createQuery(
				"from Message where id>=:firstId and contact.id=:contactId and receiverType like :contact and messageType not like :update",
				Message.class).setParameter("firstId", firstId).setParameter("contactId", contactId)
				.setParameter("contact", ReceiverType.CONTACT).setParameter("update", MessageType.UPDATE).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastPrivateMessages(Long contactId, int messageCount) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where contact.id=:contactId and receiverType like :contact and messageType not like :update order by id desc",
				Message.class).setParameter("contactId", contactId).setParameter("contact", ReceiverType.CONTACT)
				.setParameter("update", MessageType.UPDATE).setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastPrivateMessagesBeforeId(Long contactId, long messageId, int messageCount)
			throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where id<:messageId and contact.id=:contactId and receiverType like :contact and messageType not like :update order by id desc",
				Message.class).setParameter("messageId", messageId).setParameter("contactId", contactId)
				.setParameter("contact", ReceiverType.CONTACT).setParameter("update", MessageType.UPDATE)
				.setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	public Contact getContact(String uuid) throws HibernateException {

		Session session = factory.openSession();

		Contact dbContact = session.createQuery("from Contact where uuid like :uuid", Contact.class)
				.setParameter("uuid", uuid).uniqueResult();

		session.close();

		return dbContact;

	}

	public List<Message> getGroupMessagesWaitingToContact(String contactUuid) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where messageDirection like :out and waitStatus like :waiting and ((receiverType like :groupOwner and contact.uuid like :contactUuid) or (receiverType like :groupMember and statusReportStr like :statusReportStr))",
				Message.class).setParameter("out", MessageDirection.OUT).setParameter("waiting", WaitStatus.WAITING)
				.setParameter("groupOwner", ReceiverType.GROUP_OWNER).setParameter("contactUuid", contactUuid)
				.setParameter("groupMember", ReceiverType.GROUP_MEMBER)
				.setParameter("statusReportStr", String.format("%%%s%%", contactUuid)).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getGroupMessagesNotReadToItsGroup(String contactUuid) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where contact.uuid like :contactUuid and messageDirection like :out and receiverType like :groupOwner and messageStatus not like :read and waitStatus not like :canceled",
				Message.class).setParameter("contactUuid", contactUuid).setParameter("out", MessageDirection.OUT)
				.setParameter("groupOwner", ReceiverType.GROUP_OWNER).setParameter("read", MessageStatus.READ)
				.setParameter("canceled", WaitStatus.CANCELED).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getMessagesWaitingFromGroup(Long groupId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where dgroup.id=:groupId and messageDirection like :in and messageStatus not like :read and messageType not like :update",
				Message.class).setParameter("groupId", groupId).setParameter("in", MessageDirection.IN)
				.setParameter("read", MessageStatus.READ).setParameter("update", MessageType.UPDATE).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getAllGroupMessagesSinceFirstUnreadMessage(Long groupId) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbFirstUnreadMessage = session.createQuery(
				"from Message where dgroup.id=:groupId and messageDirection like :in and messageStatus not like :read and messageType not like :update",
				Message.class).setParameter("groupId", groupId).setParameter("in", MessageDirection.IN)
				.setParameter("read", MessageStatus.READ).setParameter("update", MessageType.UPDATE).setMaxResults(1)
				.list();

		if (dbFirstUnreadMessage.size() == 0) {

			return Collections.emptyList();

		}

		Long firstId = dbFirstUnreadMessage.get(0).getId();

		List<Message> dbMessages = session
				.createQuery("from Message where id>=:firstId and dgroup.id=:groupId and messageType not like :update",
						Message.class)
				.setParameter("firstId", firstId).setParameter("groupId", groupId)
				.setParameter("update", MessageType.UPDATE).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastGroupMessages(Long groupId, int messageCount) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session
				.createQuery("from Message where dgroup.id=:groupId and messageType not like :update order by id desc",
						Message.class)
				.setParameter("groupId", groupId).setParameter("update", MessageType.UPDATE).setMaxResults(messageCount)
				.list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastGroupMessagesBeforeId(Long groupId, long messageId, int messageCount)
			throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where id<:messageId and dgroup.id=:groupId and messageType not like :update order by id desc",
				Message.class).setParameter("messageId", messageId).setParameter("groupId", groupId)
				.setParameter("update", MessageType.UPDATE).setMaxResults(messageCount).list();

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

}
