package com.ogya.dms.database.tables;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageType;

@Entity
@Table(name = "message")
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "message_id")
	private Long messageId;

	@Column(name = "sender_uuid", nullable = false, updatable = false)
	private String senderUuid;

	@Column(name = "receiver_uuid", nullable = false, updatable = false)
	private String receiverUuid;

	@Column(name = "message_type", nullable = false, updatable = false)
	@Enumerated(EnumType.STRING)
	private MessageType messageType;

	@Column(name = "message_code", updatable = false)
	private Integer messageCode;

	@Column(name = "content", nullable = false, updatable = false)
	private String content;

	@Column(name = "message_status", nullable = false)
	@Enumerated(EnumType.STRING)
	private MessageStatus messageStatus;

	@Column(name = "date", nullable = false, updatable = false)
	private Date date;

	public Message() {
		super();
	}

	public Message(String senderUuid, String receiverUuid, MessageType messageType, String content) {
		this.senderUuid = senderUuid;
		this.receiverUuid = receiverUuid;
		this.messageType = messageType;
		this.content = content;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	public String getSenderUuid() {
		return senderUuid;
	}

	public void setSenderUuid(String senderUuid) {
		this.senderUuid = senderUuid;
	}

	public String getReceiverUuid() {
		return receiverUuid;
	}

	public void setReceiverUuid(String receiverUuid) {
		this.receiverUuid = receiverUuid;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public Integer getMessageCode() {
		return messageCode;
	}

	public void setMessageCode(Integer messageCode) {
		this.messageCode = messageCode;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public MessageStatus getMessageStatus() {
		return messageStatus;
	}

	public void setMessageStatus(MessageStatus messageStatus) {
		this.messageStatus = messageStatus;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@PrePersist
	protected void onCreate() {
		this.date = new Date();
	}

	@PostPersist
	protected void onPersist() {
		if (this.messageId != null)
			return;
		this.messageId = this.id;
	}

}
