package com.ogya.dms.database.tables;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageType;
import com.ogya.dms.structures.ReceiverType;
import com.ogya.dms.structures.WaitStatus;

@Entity
@Table(name = "message")
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "message_ref_id")
	@SerializedName(value = "a")
	private Long messageRefId;

	@Column(name = "message_direction", nullable = false, updatable = false)
	@Enumerated(EnumType.ORDINAL)
	@SerializedName(value = "b")
	private MessageDirection messageDirection;

	@Column(name = "receiver_type", nullable = false, updatable = false)
	@Enumerated(EnumType.ORDINAL)
	@SerializedName(value = "c")
	private ReceiverType receiverType;

	@Column(name = "message_type", nullable = false, updatable = false)
	@Enumerated(EnumType.ORDINAL)
	@SerializedName(value = "d")
	private MessageType messageType;

	@Column(name = "message_code", updatable = false)
	@SerializedName(value = "e")
	private Integer messageCode;

	@Column(name = "content", nullable = false, updatable = false, length = Integer.MAX_VALUE)
	@SerializedName(value = "f")
	private String content;

	@Column(name = "message_status", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	@SerializedName(value = "g")
	private MessageStatus messageStatus;

	@Column(name = "wait_status", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	@SerializedName(value = "h")
	private WaitStatus waitStatus;

	@Column(name = "status_report_str", length = Integer.MAX_VALUE)
	@SerializedName(value = "i")
	private String statusReportStr;

	@Column(name = "date", nullable = false, updatable = false)
	@SerializedName(value = "j")
	private Date date;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "contact_id", nullable = false, updatable = false)
	@SerializedName(value = "k")
	private Contact contact;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "dgroup_id", updatable = false)
	@SerializedName(value = "l")
	private Dgroup dgroup;

	@Transient
	@SerializedName(value = "m")
	private Long groupRefId;
	@Transient
	@SerializedName(value = "n")
	private Long contactRefId;

	public Message() {
		super();
	}

	public Message(Contact contact, Dgroup dgroup, ReceiverType receiverType, MessageType messageType, String content) {
		super();
		this.contact = contact;
		this.dgroup = dgroup;
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

	public Long getMessageRefId() {
		return messageRefId;
	}

	public void setMessageRefId(Long messageRefId) {
		this.messageRefId = messageRefId;
	}

	public MessageDirection getMessageDirection() {
		return messageDirection;
	}

	public void setMessageDirection(MessageDirection messageDirection) {
		this.messageDirection = messageDirection;
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

	public WaitStatus getWaitStatus() {
		return waitStatus;
	}

	public void setWaitStatus(WaitStatus waitStatus) {
		this.waitStatus = waitStatus;
	}

	public String getStatusReportStr() {
		return statusReportStr;
	}

	public void setStatusReportStr(String statusReportStr) {
		this.statusReportStr = statusReportStr;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Contact getContact() {
		return contact;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
	}

	public Dgroup getDgroup() {
		return dgroup;
	}

	public void setDgroup(Dgroup dgroup) {
		this.dgroup = dgroup;
	}

	public Long getGroupRefId() {
		return groupRefId;
	}

	public void setGroupRefId(Long groupRefId) {
		this.groupRefId = groupRefId;
	}

	public Long getContactRefId() {
		return contactRefId;
	}

	public void setContactRefId(Long contactRefId) {
		this.contactRefId = contactRefId;
	}

	@PrePersist
	protected void onCreate() {
		this.date = new Date();
	}

	@PostPersist
	protected void onPersist() {
		if (this.messageRefId == null)
			this.messageRefId = this.id;
	}

	public String toJson() {
		return CommonMethods.toDbJson(this);
	}

	public static Message fromJson(String json) throws Exception {
		return CommonMethods.fromDbJson(json, Message.class);
	}

}
