package com.ogya.dms.core.database.tables;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.core.structures.MessageStatus;

@Entity
@Table(name = "status_report")
public class StatusReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "contact_id", nullable = false, updatable = false)
	@SerializedName("a")
	private Long contactId;

	@Column(name = "message_status", nullable = false)
	@Enumerated(EnumType.STRING)
	@SerializedName("b")
	private MessageStatus messageStatus;

	@ManyToOne
	@JoinColumn(name = "message_id")
	private Message message;

	public StatusReport() {
		super();
	}

	public StatusReport(Long contactId, MessageStatus messageStatus) {
		super();
		this.contactId = contactId;
		this.messageStatus = messageStatus;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getContactId() {
		return contactId;
	}

	public void setContactId(Long contactId) {
		this.contactId = contactId;
	}

	public MessageStatus getMessageStatus() {
		return messageStatus;
	}

	public void setMessageStatus(MessageStatus messageStatus) {
		this.messageStatus = messageStatus;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

}
