package com.dms.core.database;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import com.dms.core.database.search.DmsAnalysisConfigurer;
import com.dms.core.database.search.DmsMappingConfigurer;
import com.dms.core.database.tables.Contact;
import com.dms.core.database.tables.ContactRef;
import com.dms.core.database.tables.Dgroup;
import com.dms.core.database.tables.Message;
import com.dms.core.database.tables.StatusReport;
import com.dms.core.structures.Availability;
import com.dms.core.structures.MessageStatus;
import com.dms.core.structures.ViewStatus;
import com.dms.core.util.Commons;

public class DbManager {

	private static final Map<String, Semaphore> LOCKS = Collections.synchronizedMap(new HashMap<String, Semaphore>());

	private final String name;

	private final SessionFactory factory;

	public DbManager(String dbName, String dbPassword) throws Exception {

		name = dbName;

		if (name.length() > 40) {
			throw new Exception("Name too long, cannot exceed 40 characters.");
		}

		try {

			lock();

			String dbPath = Commons.DB_PATH + File.separator + dbName;

			StandardServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
					.configure("/resources/hibernate.cfg/dms.cfg.xml")
					.applySetting("hibernate.connection.url", "jdbc:h2:" + dbPath)
					.applySetting("hibernate.connection.username", dbName)
					.applySetting("hibernate.connection.password", dbPassword)
					.applySetting("hibernate.search.backend.directory.root", dbPath)
					.applySetting("hibernate.search.backend.lucene_version", "LUCENE_8_11_2")
					.applySetting("hibernate.search.backend.analysis.configurer",
							"class:" + DmsAnalysisConfigurer.class.getName())
					.applySetting("hibernate.search.mapping.process_annotations", false)
					.applySetting("hibernate.search.mapping.configurer",
							"class:" + DmsMappingConfigurer.class.getName())
					.build();
			MetadataBuilder metadataBuilder = new MetadataSources(standardRegistry).addAnnotatedClass(Contact.class)
					.addAnnotatedClass(ContactRef.class).addAnnotatedClass(Dgroup.class)
					.addAnnotatedClass(Message.class).addAnnotatedClass(StatusReport.class).getMetadataBuilder();
			Metadata metadata = metadataBuilder
					.applyImplicitNamingStrategy(ImplicitNamingStrategyJpaCompliantImpl.INSTANCE).build();
			SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
			factory = sessionFactoryBuilder.build();

			Runtime.getRuntime().addShutdownHook(new Thread(() -> close()));

		} catch (Exception e) {
			unlock();
			throw e;
		}

	}

	private void lock() {
		Semaphore lock = LOCKS.get(name);
		if (lock == null) {
			lock = new Semaphore(1);
			LOCKS.put(name, lock);
		}
		lock.acquireUninterruptibly();
	}

	private void unlock() {
		Semaphore lock = LOCKS.remove(name);
		if (lock == null) {
			return;
		}
		lock.release();
	}

	public void close() {
		factory.close();
		unlock();
	}

	public Contact getIdentity() throws HibernateException {

		Session session = factory.openSession();
		Transaction tx = null;

		try {

			Contact identity = session.createQuery("from Contact", Contact.class).setMaxResults(1).uniqueResult();

			if (identity == null) {

				identity = new Contact(UUID.randomUUID().toString());

				identity.setName(name);

				identity.setStatus(Availability.AVAILABLE);

				tx = session.beginTransaction();

				session.persist(identity);

				tx.commit();

			}

			return identity;

		} catch (HibernateException e) {

			if (tx != null) {
				tx.rollback();
			}

			throw e;

		} finally {

			session.close();

		}

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
		Transaction tx = null;

		try {

			tx = session.beginTransaction();

			Contact newIdentity = (Contact) session.merge(identity);

			tx.commit();

			return newIdentity;

		} catch (HibernateException e) {

			if (tx != null) {
				tx.rollback();
			}

			throw e;

		} finally {

			session.close();

		}

	}

	public Contact addUpdateContact(Contact contact) throws HibernateException {

		Session session = factory.openSession();
		Transaction tx = null;

		try {

			Contact dbContact = session.createQuery("from Contact where uuid like :uuid", Contact.class)
					.setParameter("uuid", contact.getUuid()).uniqueResult();

			tx = session.beginTransaction();

			if (dbContact == null) {

				dbContact = contact;

				dbContact.setId(null);

				session.persist(dbContact);

			} else {

				contact.setId(dbContact.getId());

				dbContact = (Contact) session.merge(contact);

			}

			tx.commit();

			return dbContact;

		} catch (HibernateException e) {

			if (tx != null) {
				tx.rollback();
			}

			throw e;

		} finally {

			session.close();

		}

	}

	public Contact getContact(String uuid) throws HibernateException {

		Session session = factory.openSession();

		Contact dbContact = session.createQuery("from Contact where uuid like :uuid", Contact.class)
				.setParameter("uuid", uuid).uniqueResult();

		session.close();

		return dbContact;

	}

	public Contact getContactById(Long id) throws HibernateException {

		Session session = factory.openSession();

		Contact dbContact = session.createQuery("from Contact where id=:id", Contact.class).setParameter("id", id)
				.uniqueResult();

		session.close();

		return dbContact;

	}

	public Dgroup addUpdateGroup(Dgroup group) throws HibernateException {

		Session session = factory.openSession();
		Transaction tx = null;

		try {

			Dgroup dbGroup = session
					.createQuery("from Dgroup where owner.uuid like :ownerUuid and groupRefId=:groupRefId",
							Dgroup.class)
					.setParameter("ownerUuid", group.getOwner().getUuid())
					.setParameter("groupRefId", group.getGroupRefId()).uniqueResult();

			tx = session.beginTransaction();

			if (dbGroup == null) {

				dbGroup = group;

				dbGroup.setId(null);

				session.persist(dbGroup);

				if (dbGroup.getGroupRefId() == null) {
					dbGroup.setGroupRefId(dbGroup.getId());
					session.persist(dbGroup);
				}

			} else {

				group.setId(dbGroup.getId());

				dbGroup = (Dgroup) session.merge(group);

			}

			tx.commit();

			return dbGroup;

		} catch (HibernateException e) {

			if (tx != null) {
				tx.rollback();
			}

			throw e;

		} finally {

			session.close();

		}

	}

	public Dgroup getGroupById(Long id) {

		Session session = factory.openSession();

		Dgroup dbGroup = session.createQuery("from Dgroup where id=:id", Dgroup.class).setParameter("id", id)
				.uniqueResult();

		session.close();

		return dbGroup;

	}

	public Dgroup getGroupByOwner(String ownerUuid, Long groupRefId) {

		Session session = factory.openSession();

		Dgroup dbGroup = session
				.createQuery("from Dgroup where owner.uuid like :ownerUuid and groupRefId=:groupRefId", Dgroup.class)
				.setParameter("ownerUuid", ownerUuid).setParameter("groupRefId", groupRefId).uniqueResult();

		session.close();

		return dbGroup;

	}

	public ContactRef addUpdateContactRef(ContactRef contactRef) throws HibernateException {

		Session session = factory.openSession();
		Transaction tx = null;

		try {

			ContactRef dbContactRef = session
					.createQuery("from ContactRef where owner=:owner and contactRefId=:contactRefId", ContactRef.class)
					.setParameter("owner", contactRef.getOwner())
					.setParameter("contactRefId", contactRef.getContactRefId()).uniqueResult();

			tx = session.beginTransaction();

			if (dbContactRef == null) {

				dbContactRef = contactRef;

				dbContactRef.setId(null);

				session.persist(dbContactRef);

			} else {

				contactRef.setId(dbContactRef.getId());

				dbContactRef = (ContactRef) session.merge(contactRef);

			}

			tx.commit();

			return dbContactRef;

		} catch (HibernateException e) {

			if (tx != null) {
				tx.rollback();
			}

			throw e;

		} finally {

			session.close();

		}

	}

	public ContactRef getContactRef(String ownerUuid, Long contactRefId) throws HibernateException {

		if (ownerUuid == null || contactRefId == null) {
			return null;
		}

		Session session = factory.openSession();

		ContactRef dbContactRef = session
				.createQuery("from ContactRef where owner.uuid like :ownerUuid and contactRefId=:contactRefId",
						ContactRef.class)
				.setParameter("ownerUuid", ownerUuid).setParameter("contactRefId", contactRefId).uniqueResult();

		session.close();

		return dbContactRef;

	}

	public Message addUpdateMessage(Message message) throws HibernateException {

		Session session = factory.openSession();
		Transaction tx = null;

		try {

			tx = session.beginTransaction();

			if (message.getId() == null) {

				resolveReferenceOfMessage(message, session);

				session.persist(message);

			} else {

				message = (Message) session.merge(message);

			}

			tx.commit();

			return message;

		} catch (HibernateException e) {

			if (tx != null) {
				tx.rollback();
			}

			throw e;

		} finally {

			session.close();

		}

	}

	public List<Message> addUpdateMessages(List<Message> messages) throws HibernateException {

		Session session = factory.openSession();
		Transaction tx = null;

		try {

			List<Message> dbMessages = new ArrayList<Message>();

			tx = session.beginTransaction();

			for (Message message : messages) {

				if (message.getId() == null) {

					resolveReferenceOfMessage(message, session);

					session.persist(message);

				} else {

					message = (Message) session.merge(message);

				}

				dbMessages.add(message);

			}

			tx.commit();

			return dbMessages;

		} catch (HibernateException e) {

			if (tx != null) {
				tx.rollback();
			}

			throw e;

		} finally {

			session.close();

		}

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

		if (id == null) {
			return null;
		}

		Session session = factory.openSession();

		Message dbMessage = session.createQuery("from Message where id=:id", Message.class).setParameter("id", id)
				.uniqueResult();

		session.close();

		return dbMessage;

	}

	public List<Message> getMessagesById(Long[] ids) throws HibernateException {

		if (ids == null) {
			return null;
		}

		Session session = factory.openSession();

		List<Message> dbMessages = new ArrayList<Message>();

		for (Long id : ids) {

			if (id == null) {
				continue;
			}

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

	public List<Message> getAllDeletablePrivateMessages(Long contactId) {

		Session session = factory.openSession();

		List<Message> dbMessages = session.createQuery(
				"from Message where viewStatus like :default and contact.id=:contactId and dgroup is null and updateType is null",
				Message.class).setParameter("default", ViewStatus.DEFAULT).setParameter("contactId", contactId).list();

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

	public List<Message> getAllDeletableGroupMessages(Long groupId) {

		Session session = factory.openSession();

		List<Message> dbMessages = session
				.createQuery(
						"from Message where viewStatus like :default and dgroup.id=:groupId and updateType is null",
						Message.class)
				.setParameter("default", ViewStatus.DEFAULT).setParameter("groupId", groupId).list();

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

	public List<Message> searchInPrivateConversation(Long contactId, String fulltext) throws HibernateException {

		Session session = factory.openSession();

		SearchSession searchSession = Search.session(session);

		List<Message> hits = searchSession.search(Message.class)
				.where(f -> f.bool().must(f.simpleQueryString().field("content").matching(fulltext.trim() + "*"))
						.must(f.match().field("contact.id").matching(contactId)).mustNot(f.exists().field("dgroup")))
				.sort(f -> f.field("id").desc()).fetchAllHits();

		session.close();

		return hits;

	}

	public List<Message> searchInGroupConversation(Long groupId, String fulltext) throws HibernateException {

		Session session = factory.openSession();

		SearchSession searchSession = Search.session(session);

		List<Message> hits = searchSession.search(Message.class)
				.where(f -> f.bool().must(f.simpleQueryString().field("content").matching(fulltext.trim() + "*"))
						.must(f.match().field("dgroup.id").matching(groupId)))
				.sort(f -> f.field("id").desc()).fetchAllHits();

		session.close();

		return hits;

	}

	public List<Message> searchInArchivedMessages(String fulltext) throws HibernateException {

		Session session = factory.openSession();

		SearchSession searchSession = Search.session(session);

		List<Message> hits = searchSession.search(Message.class)
				.where(f -> f.bool().must(f.simpleQueryString().field("content").matching(fulltext.trim() + "*"))
						.must(f.match().field("viewStatus").matching(ViewStatus.ARCHIVED)))
				.sort(f -> f.field("id").desc()).fetchAllHits();

		session.close();

		return hits;

	}

	public List<Message> searchInAllMessages(String fulltext) throws HibernateException {

		Session session = factory.openSession();

		SearchSession searchSession = Search.session(session);

		List<Message> hits = searchSession.search(Message.class)
				.where(f -> f.simpleQueryString().field("content").matching(fulltext.trim() + "*"))
				.sort(f -> f.field("id").desc()).fetchAllHits();

		session.close();

		return hits;

	}

	private void resolveReferenceOfMessage(Message message, Session session) throws HibernateException {

		Message refMessage = message.getRefMessage();

		if (refMessage == null || message.isLocal()) {
			return;
		}

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
