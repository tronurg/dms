package com.dms.core.database.tables;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.dms.core.database.converters.AvailabilityConverter;
import com.dms.core.database.converters.ViewStatusConverter;
import com.dms.core.structures.Availability;
import com.dms.core.structures.ViewStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "dgroup")
public class Dgroup extends DmsEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_gen")
	@SequenceGenerator(name = "entity_gen", sequenceName = "entity_seq", initialValue = 1, allocationSize = 1)
	private Long id;

	@Column(name = "group_ref_id")
	private Long groupRefId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "comment", length = 1000000000)
	private String comment;

	@Column(name = "status", nullable = false)
	@Convert(converter = AvailabilityConverter.class)
	private Availability status;

	@Column(name = "view_status", nullable = false)
	@Convert(converter = ViewStatusConverter.class)
	@JsonIgnore
	private ViewStatus viewStatus;

	@Column(name = "active", nullable = false)
	private boolean active;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "owner_id", nullable = false, updatable = false)
	private Contact owner;

	@Column(name = "local", nullable = false, updatable = false)
	private boolean local = false;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "dgroup_members", joinColumns = { @JoinColumn(name = "dgroup_id") }, inverseJoinColumns = {
			@JoinColumn(name = "member_id") })
	private final Set<Contact> members = new HashSet<Contact>();

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

	public ViewStatus getViewStatus() {
		return viewStatus;
	}

	public void setViewStatus(ViewStatus viewStatus) {
		this.viewStatus = viewStatus;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Contact getOwner() {
		return owner;
	}

	public void setOwner(Contact owner) {
		this.owner = owner;
	}

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	public Set<Contact> getMembers() {
		return members;
	}

	@PrePersist
	protected void prePersist() {
		if (name == null || name.isEmpty()) {
			name = owner.getName();
		}
		preUpdate();
	}

	@PreUpdate
	protected void preUpdate() {
		if (viewStatus == null || status != Availability.OFFLINE) {
			viewStatus = ViewStatus.DEFAULT;
		}
	}

	@Override
	public boolean isGroup() {
		return true;
	}

	@Override
	public Double getLatitude() {
		return null;
	}

	@Override
	public Double getLongitude() {
		return null;
	}

}
