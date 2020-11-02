package com.ogya.dms.database.tables;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import com.ogya.dms.structures.Availability;

@Entity
@Table(name = "dgroup")
public class Dgroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "uuid", nullable = false, updatable = false)
	private String uuid;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "comment")
	private String comment;

	@Column(name = "owner_uuid", nullable = false, updatable = false)
	private String ownerUuid;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	private Availability status;

	@Column(name = "active", nullable = false)
	private Boolean active;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "dgroup_contacts", joinColumns = { @JoinColumn(name = "dgroup_id") }, inverseJoinColumns = {
			@JoinColumn(name = "contact_id") })
	private Set<Contact> contacts = new HashSet<Contact>();

	public Dgroup() {
		super();
	}

	public Dgroup(String ownerUuid) {
		super();
		this.ownerUuid = ownerUuid;
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

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getOwnerUuid() {
		return ownerUuid;
	}

	public void setOwnerUuid(String ownerUuid) {
		this.ownerUuid = ownerUuid;
	}

	public Availability getStatus() {
		return status;
	}

	public void setStatus(Availability status) {
		this.status = status;
	}

	public Boolean isActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Set<Contact> getContacts() {
		return contacts;
	}

	public void setContacts(Set<Contact> contacts) {
		this.contacts = contacts;
	}

	@PrePersist
	private void onCreate() {
		if (this.uuid == null)
			this.uuid = UUID.randomUUID().toString();
	}

}
