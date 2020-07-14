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

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageType;
import com.ogya.dms.structures.ReceiverType;

@Entity
@Table(name = "message")
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "message_id")
	private Long messageId;

	@Column(name = "owner_uuid", nullable = false, updatable = false)
	private String ownerUuid;

	@Column(name = "sender_uuid", nullable = false, updatable = false)
	private String senderUuid;

	@Column(name = "receiver_uuid", nullable = false, updatable = false)
	private String receiverUuid;

	@Column(name = "receiver_type", nullable = false, updatable = false)
	@Enumerated(EnumType.STRING)
	private ReceiverType receiverType;

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

	@Column(name = "status_report_str")
	private String statusReportStr;

	@Column(name = "waiting", nullable = false)
	private Boolean waiting;

	@Column(name = "date", nullable = false, updatable = false)
	private Date date;

	public Message() {
		super();
	}

	public Message(String ownerUuid, String receiverUuid, ReceiverType receiverType, MessageType messageType,
			String content) {
		this.ownerUuid = ownerUuid;
		this.receiverUuid = receiverUuid;
		this.receiverType = receiverType;
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

	public String getOwnerUuid() {
		return ownerUuid;
	}

	public void setOwnerUuid(String ownerUuid) {
		this.ownerUuid = ownerUuid;
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

	public ReceiverType getReceiverType() {
		return receiverType;
	}

	public void setReceiverType(ReceiverType receiverType) {
		this.receiverType = receiverType;
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

	public String getStatusReportStr() {
		return statusReportStr;
	}

	public void setStatusReportStr(String statusReportStr) {
		this.statusReportStr = statusReportStr;
	}

	public Boolean isWaiting() {
		return waiting;
	}

	public void setWaiting(Boolean waiting) {
		this.waiting = waiting;
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
		if (this.messageId == null)
			this.messageId = this.id;
	}

	public String toJson() {
		return CommonMethods.toDbJson(this);
	}

	public static Message fromJson(String json) throws Exception {
		return CommonMethods.fromDbJson(json, Message.class);
	}

}
