package com.ogya.dms.database.tables;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "dgroup")
public class Dgroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "uuid", nullable = false, updatable = false)
	private String uuid;

	@Column(name = "name", nullable = false, updatable = false)
	private String name;

	@Column(name = "uuid_creator", nullable = false, updatable = false)
	private String uuidCreator;

	@ManyToMany
	@JoinTable(name = "dgroup_contacts", joinColumns = { @JoinColumn(name = "dgroup_id") }, inverseJoinColumns = {
			@JoinColumn(name = "contact_id") })
	private Set<Contact> contacts = new HashSet<Contact>();

	public Dgroup() {
		super();
	}

	public Dgroup(String name, String uuidCreator) {
		this.name = name;
		this.uuidCreator = uuidCreator;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUuidCreator() {
		return uuidCreator;
	}

	public void setUuidCreator(String uuidCreator) {
		this.uuidCreator = uuidCreator;
	}

	public Set<Contact> getContacts() {
		return contacts;
	}

	public void setContacts(Set<Contact> contacts) {
		this.contacts = contacts;
	}

	@PrePersist
	private void onCreate() {
		this.uuid = UUID.randomUUID().toString();
	}

}
