package com.ogya.dms.core.database.tables;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ogya.dms.core.database.converters.AvailabilityConverter;
import com.ogya.dms.core.database.converters.ViewStatusConverter;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.ViewStatus;

@Entity
@Table(name = "contact")
public class Contact extends EntityBase {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_gen")
	@SequenceGenerator(name = "entity_gen", sequenceName = "entity_seq", initialValue = 1, allocationSize = 1)
	private Long id;

	@Column(name = "uuid", unique = true, nullable = false, updatable = false)
	private String uuid;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "comment")
	private String comment;

	@Column(name = "status", nullable = false)
	@Convert(converter = AvailabilityConverter.class)
	private Availability status;

	@Column(name = "view_status", nullable = false)
	@Convert(converter = ViewStatusConverter.class)
	@JsonIgnore
	private ViewStatus viewStatus;

	@Column(name = "lattitude")
	private Double lattitude;

	@Column(name = "longitude")
	private Double longitude;

	@Column(name = "secret_id")
	private String secretId;

	@Transient
	private final Map<InetAddress, InetAddress> localRemoteServerIps = new HashMap<InetAddress, InetAddress>();

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

	public ViewStatus getViewStatus() {
		return viewStatus;
	}

	public void setViewStatus(ViewStatus viewStatus) {
		this.viewStatus = viewStatus;
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

	public Map<InetAddress, InetAddress> getLocalRemoteServerIps() {
		return localRemoteServerIps;
	}

	public void setLocalRemoteServerIps(Map<InetAddress, InetAddress> localRemoteServerIps) {
		this.localRemoteServerIps.clear();
		if (localRemoteServerIps != null)
			this.localRemoteServerIps.putAll(localRemoteServerIps);
	}

	@PrePersist
	protected void prePersist() {
		if (name == null || name.isEmpty())
			name = uuid;
		preUpdate();
	}

	@PreUpdate
	protected void preUpdate() {
		if (viewStatus == null || !Objects.equals(status, Availability.OFFLINE))
			viewStatus = ViewStatus.DEFAULT;
	}

	@Override
	public EntityId getEntityId() {
		return EntityId.of(id, false);
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
