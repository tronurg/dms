package com.ogya.dms.database.tables;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import com.ogya.dms.structures.ContactStatus;

@Entity
@Table(name = "contact")
public class Contact {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "uuid", unique = true, nullable = false, updatable = false)
	private String uuid;

	@Column(name = "name", nullable = false, updatable = false)
	private String name;

	@Column(name = "comment")
	private String comment;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private ContactStatus status;

	@Column(name = "lattitude")
	private Double lattitude;

	@Column(name = "longitude")
	private Double longitude;

	@ManyToMany(mappedBy = "contacts")
	private Set<Group> groups = new HashSet<Group>();

	public Contact() {
		super();
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

	public ContactStatus getStatus() {
		return status;
	}

	public void setStatus(ContactStatus status) {
		this.status = status;
	}

	public Double getLattitude() {
		return lattitude;
	}

	public void setLattitude(Double lattitude) {
		this.lattitude = lattitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Set<Group> getGroups() {
		return groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}

}
