package com.ogya.dms.database.tables;

import java.util.HashSet;
import java.util.Set;

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
import javax.persistence.ManyToOne;
import javax.persistence.PostPersist;
import javax.persistence.Table;

import com.ogya.dms.structures.Availability;

@Entity
@Table(name = "dgroup")
public class Dgroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_ref_id")
	private Long groupRefId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "comment", length = Integer.MAX_VALUE)
	private String comment;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	private Availability status;

	@Column(name = "active", nullable = false)
	private Boolean active;

	@Column(name = "contact_map_str", length = Integer.MAX_VALUE)
	private String contactMapStr;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "dgroup_id", nullable = false, updatable = false)
	private Contact owner;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "dgroup_members", joinColumns = { @JoinColumn(name = "dgroup_id") }, inverseJoinColumns = {
			@JoinColumn(name = "member_id") })
	private Set<Contact> members = new HashSet<Contact>();

	public Dgroup() {
		super();
	}

	public Dgroup(Contact owner, Long groupRefId) {
		super();
		this.owner = owner;
		this.groupRefId = groupRefId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getGroupRefId() {
		return groupRefId;
	}

	public void setGroupRefId(Long groupRefId) {
		this.groupRefId = groupRefId;
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

	public Availability getStatus() {
		return status;
	}

	public void setStatus(Availability status) {
		this.status = status;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public String getContactMapStr() {
		return contactMapStr;
	}

	public void setContactMapStr(String contactMapStr) {
		this.contactMapStr = contactMapStr;
	}

	public Contact getOwner() {
		return owner;
	}

	public void setOwner(Contact owner) {
		this.owner = owner;
	}

	public Set<Contact> getMembers() {
		return members;
	}

	public void setMembers(Set<Contact> members) {
		this.members = members;
	}

	@PostPersist
	protected void onPersist() {
		if (this.groupRefId == null)
			this.groupRefId = this.id;
	}

}
