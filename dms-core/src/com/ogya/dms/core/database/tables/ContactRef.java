package com.ogya.dms.core.database.tables;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "contact_ref")
public class ContactRef {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contact_ref_gen")
	@SequenceGenerator(name = "contact_ref_gen", sequenceName = "contact_ref_seq", initialValue = 1, allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "owner_id", nullable = false, updatable = false)
	private Contact owner;

	@Column(name = "contact_ref_id", nullable = false, updatable = false)
	private Long contactRefId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "contact_id", nullable = false, updatable = false)
	private Contact contact;

	public ContactRef() {
		super();
	}

	public ContactRef(Contact owner, Long contactRefId, Contact contact) {
		super();
		this.owner = owner;
		this.contactRefId = contactRefId;
		this.contact = contact;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Contact getOwner() {
		return owner;
	}

	public void setOwner(Contact owner) {
		this.owner = owner;
	}

	public Long getContactRefId() {
		return contactRefId;
	}

	public void setContactRefId(Long contactRefId) {
		this.contactRefId = contactRefId;
	}

	public Contact getContact() {
		return contact;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
	}

}
