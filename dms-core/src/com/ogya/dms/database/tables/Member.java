package com.ogya.dms.database.tables;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "member")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "owner_id", nullable = false, updatable = false)
	private Contact owner;

	@Column(name = "contact_ref_id", nullable = false, updatable = false)
	private Long contactRefId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "contact_id", nullable = false, updatable = false)
	private Contact contact;

	public Member() {
		super();
	}

	public Member(Contact owner, Long contactRefId, Contact contact) {
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
