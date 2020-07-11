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

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.structures.Availability;

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
	private Availability status;

	@Column(name = "lattitude")
	private Double lattitude;

	@Column(name = "longitude")
	private Double longitude;

	@ManyToMany(mappedBy = "contacts")
	private Set<Dgroup> groups = new HashSet<Dgroup>();

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

	public Availability getStatus() {
		return status;
	}

	public void setStatus(Availability status) {
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

	public Set<Dgroup> getGroups() {
		return groups;
	}

	public void setGroups(Set<Dgroup> groups) {
		this.groups = groups;
	}

	public String toJson() {
		return CommonMethods.toDbJson(this);
	}

	public static Contact fromJson(String json) throws Exception {
		return CommonMethods.fromDbJson(json, Contact.class);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Contact))
			return false;
		Contact contact = (Contact) obj;
		return this.uuid.equals(contact.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

}
