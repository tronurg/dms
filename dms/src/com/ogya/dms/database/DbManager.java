package com.ogya.dms.database;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import com.ogya.dms.common.CommonConstants;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.database.tables.Identity;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.intf.exceptions.DbException;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageType;
import com.ogya.dms.structures.ReceiverType;

public class DbManager {

	private final String name;

	private final SessionFactory factory;

	public DbManager(String dbName, String dbPassword) throws DbException {

		name = dbName;

		try {

			factory = new Configuration().configure(new File("./plugins/hibernate.cfg/mcsy.cfg.xml"))
					.setProperty("hibernate.connection.url",
							"jdbc:h2:" + CommonConstants.DB_PATH + File.separator + dbName)
					.setProperty("hibernate.connection.username", dbName)
					.setProperty("hibernate.connection.password", dbPassword).addAnnotatedClass(Dgroup.class)
					.addAnnotatedClass(Identity.class).addAnnotatedClass(Contact.class).addAnnotatedClass(Message.class)
					.buildSessionFactory();

			Runtime.getRuntime().addShutdownHook(new Thread(() -> factory.close()));

		} catch (HibernateException e) {

			e.printStackTrace();

			throw new DbException("Database cannot be accessed. Wrong password or account in use.");

		}

	}

	public Identity getIdentity() throws HibernateException {

		Session session = factory.openSession();

		Identity identity = session.createQuery("from Identity where name like :name", Identity.class)
				.setParameter("name", name).uniqueResult();

		if (identity == null) {

			identity = new Identity(name);

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

	public Identity updateIdentity(Identity identity) throws HibernateException {

		Session session = factory.openSession();

		session.beginTransaction();

		Identity newIdentity = (Identity) session.merge(identity);

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

		Dgroup dbGroup = session.createQuery("from Dgroup where uuid like :uuid", Dgroup.class)
				.setParameter("uuid", group.getUuid()).uniqueResult();

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

		Message dbMessage = session
				.createQuery("from Message where senderUuid like :senderUuid and messageId=:messageId", Message.class)
				.setParameter("senderUuid", message.getSenderUuid()).setParameter("messageId", message.getMessageId())
				.uniqueResult();

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

	public Message getMessage(String senderUuid, long messageId) throws HibernateException {

		Session session = factory.openSession();

		Message dbMessage = session
				.createQuery("from Message where senderUuid like :senderUuid and messageId=:messageId", Message.class)
				.setParameter("senderUuid", senderUuid).setParameter("messageId", messageId).uniqueResult();

		session.close();

		return dbMessage;

	}

	public Message getMessage(long id) throws HibernateException {

		Session session = factory.openSession();

		Message dbMessage = session.createQuery("from Message where id like :id", Message.class).setParameter("id", id)
				.uniqueResult();

		session.close();

		return dbMessage;

	}

	public List<Message> getPrivateMessagesWaitingToContact(String receiverUuid) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where receiverUuid like :receiverUuid and receiverType like :receiverType and waiting=:waiting",
				Message.class).setParameter("receiverUuid", receiverUuid)
				.setParameter("receiverType", ReceiverType.PRIVATE).setParameter("waiting", true).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getPrivateMessagesWaitingFromContact(String senderUuid) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where senderUuid like :senderUuid and messageStatus like :received and receiverType like :receiverType",
				Message.class).setParameter("senderUuid", senderUuid).setParameter("received", MessageStatus.RECEIVED)
				.setParameter("receiverType", ReceiverType.PRIVATE).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getAllPrivateMessagesSinceFirstUnreadMessage(String localUuid, String remoteUuid)
			throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbFirstUnreadMessage = session.createQuery(
				"from Message where senderUuid like :remoteUuid and receiverUuid like :localUuid and receiverType like :receiverType and messageStatus not like :read",
				Message.class).setParameter("localUuid", localUuid).setParameter("remoteUuid", remoteUuid)
				.setParameter("receiverType", ReceiverType.PRIVATE).setParameter("read", MessageStatus.READ)
				.setMaxResults(1).list();

		if (dbFirstUnreadMessage.size() == 0) {

			return Collections.emptyList();

		}

		Long firstId = dbFirstUnreadMessage.get(0).getId();

		List<Message> dbMessages = session.createQuery(
				"from Message where id>=:firstId and ((senderUuid like :localUuid and receiverUuid like :remoteUuid) or (senderUuid like :remoteUuid and receiverUuid like :localUuid)) and receiverType like :receiverType",
				Message.class).setParameter("firstId", firstId).setParameter("localUuid", localUuid)
				.setParameter("remoteUuid", remoteUuid).setParameter("receiverType", ReceiverType.PRIVATE).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastPrivateMessages(String localUuid, String remoteUuid, int messageCount)
			throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where ((senderUuid like :localUuid and receiverUuid like :remoteUuid) or (senderUuid like :remoteUuid and receiverUuid like :localUuid)) and receiverType like :receiverType order by id desc",
				Message.class).setParameter("localUuid", localUuid).setParameter("remoteUuid", remoteUuid)
				.setParameter("receiverType", ReceiverType.PRIVATE).setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastPrivateMessagesBeforeId(String localUuid, String remoteUuid, long id, int messageCount)
			throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where id<:id and ((senderUuid like :localUuid and receiverUuid like :remoteUuid) or (senderUuid like :remoteUuid and receiverUuid like :localUuid)) and receiverType like :receiverType order by id desc",
				Message.class).setParameter("id", id).setParameter("localUuid", localUuid)
				.setParameter("remoteUuid", remoteUuid).setParameter("receiverType", ReceiverType.PRIVATE)
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

	public List<Message> getGroupMessagesWaitingToContact(String receiverUuid) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where receiverType like :receiverType and statusReportStr like :statusReportStr and waiting=:waiting",
				Message.class).setParameter("receiverType", ReceiverType.GROUP)
				.setParameter("statusReportStr", String.format("%%%s%%", receiverUuid)).setParameter("waiting", true)
				.list();

		session.close();

		return dbMessages;

	}

	public List<Message> getGroupMessagesNotReadToItsGroup(String senderUuid, String ownerUuid)
			throws HibernateException {

		Session session = factory.openSession();

		List<String> dbGroupUuids = session
				.createQuery("select uuid from Dgroup where ownerUuid like :ownerUuid", String.class)
				.setParameter("ownerUuid", ownerUuid).list();

		List<Message> dbMessages = session.createQuery(
				"from Message where senderUuid like :senderUuid and receiverUuid in (:groupUuids) and receiverType like :receiverType and messageStatus not like :read",
				Message.class).setParameter("senderUuid", senderUuid).setParameterList("groupUuids", dbGroupUuids)
				.setParameter("receiverType", ReceiverType.GROUP).setParameter("read", MessageStatus.READ).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getMessagesWaitingFromGroup(String localUuid, String groupUuid) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where senderUuid not like :localUuid and receiverUuid like :groupUuid and receiverType like :receiverType and messageStatus not like :read and messageType not like :messageType",
				Message.class).setParameter("localUuid", localUuid).setParameter("groupUuid", groupUuid)
				.setParameter("receiverType", ReceiverType.GROUP).setParameter("read", MessageStatus.READ)
				.setParameter("messageType", MessageType.UPDATE).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getAllGroupMessagesSinceFirstUnreadMessage(String localUuid, String groupUuid)
			throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbFirstUnreadMessage = session.createQuery(
				"from Message where senderUuid not like :localUuid and receiverUuid like :groupUuid and receiverType like :receiverType and messageStatus not like :read and messageType not like :messageType",
				Message.class).setParameter("localUuid", localUuid).setParameter("groupUuid", groupUuid)
				.setParameter("receiverType", ReceiverType.GROUP).setParameter("read", MessageStatus.READ)
				.setParameter("messageType", MessageType.UPDATE).setMaxResults(1).list();

		if (dbFirstUnreadMessage.size() == 0) {

			return Collections.emptyList();

		}

		Long firstId = dbFirstUnreadMessage.get(0).getId();

		List<Message> dbMessages = session.createQuery(
				"from Message where id>=:firstId and receiverUuid like :groupUuid and receiverType like :receiverType and messageType not like :messageType",
				Message.class).setParameter("firstId", firstId).setParameter("groupUuid", groupUuid)
				.setParameter("receiverType", ReceiverType.GROUP).setParameter("messageType", MessageType.UPDATE)
				.list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastGroupMessages(String groupUuid, int messageCount) throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where receiverUuid like :groupUuid and receiverType like :receiverType and messageType not like :messageType order by id desc",
				Message.class).setParameter("groupUuid", groupUuid).setParameter("receiverType", ReceiverType.GROUP)
				.setParameter("messageType", MessageType.UPDATE).setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	public List<Message> getLastGroupMessagesBeforeId(String groupUuid, long id, int messageCount)
			throws HibernateException {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where id<:id and receiverUuid like :groupUuid and receiverType like :receiverType and messageType not like :messageType order by id desc",
				Message.class).setParameter("id", id).setParameter("groupUuid", groupUuid)
				.setParameter("receiverType", ReceiverType.GROUP).setParameter("messageType", MessageType.UPDATE)
				.setMaxResults(messageCount).list();

		session.close();

		return dbMessages;

	}

	public List<Dgroup> getAllActiveGroupsOfUuid(String ownerUuid) throws HibernateException {

		Session session = factory.openSession();

		List<Dgroup> dbGroups = session
				.createQuery("from Dgroup where ownerUuid like :ownerUuid and active=true", Dgroup.class)
				.setParameter("ownerUuid", ownerUuid).list();

		session.close();

		return dbGroups;

	}

}
