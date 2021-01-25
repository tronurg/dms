package com.ogya.dms.core.database.tables;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ogya.dms.core.structures.Availability;

@Entity
@Table(name = "contact")
public class Contact {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "uuid", unique = true, nullable = false, updatable = false)
	private String uuid;

	@Column(name = "name")
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

	@Column(name = "secret_id")
	private String secretId;

	@Transient
	private final List<InetAddress> remoteInterfaces = new ArrayList<InetAddress>();
	@Transient
	private final List<InetAddress> localInterfaces = new ArrayList<InetAddress>();

	public Contact() {
		super();
	}

	public Contact(String uuid) {
		super();
		this.uuid = uuid;
	}

	public Contact(String uuid, String name, String comment, Availability status, Double lattitude, Double longitude,
			String secretId) {
		super();
		this.uuid = uuid;
		this.name = name;
		this.comment = comment;
		this.status = status;
		this.lattitude = lattitude;
		this.longitude = longitude;
		this.secretId = secretId;
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

	public String getSecretId() {
		return secretId;
	}

	public void setSecretId(String secretId) {
		this.secretId = secretId;
	}

	public List<InetAddress> getRemoteInterfaces() {
		return remoteInterfaces;
	}

	public void setRemoteInterfaces(List<InetAddress> remoteInterfaces) {
		this.remoteInterfaces.clear();
		if (remoteInterfaces != null)
			this.remoteInterfaces.addAll(remoteInterfaces);
	}

	public List<InetAddress> getLocalInterfaces() {
		return localInterfaces;
	}

	public void setLocalInterfaces(List<InetAddress> localInterfaces) {
		this.localInterfaces.clear();
		if (localInterfaces != null)
			this.localInterfaces.addAll(localInterfaces);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Contact))
			return false;
		Contact contact = (Contact) obj;
		return Objects.equals(this.uuid, contact.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

}
