package com.ogya.dms.core.database.tables;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ogya.dms.core.database.converters.MessageDirectionConverter;
import com.ogya.dms.core.database.converters.MessageStatusConverter;
import com.ogya.dms.core.database.converters.MessageSubTypeConverter;
import com.ogya.dms.core.database.converters.MessageTypeConverter;
import com.ogya.dms.core.database.converters.ReceiverTypeConverter;
import com.ogya.dms.core.database.converters.WaitStatusConverter;
import com.ogya.dms.core.structures.MessageDirection;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.MessageSubType;
import com.ogya.dms.core.structures.MessageType;
import com.ogya.dms.core.structures.ReceiverType;
import com.ogya.dms.core.structures.WaitStatus;

@Entity
@Table(name = "message")
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("a")
	private Long id;

	@Column(name = "message_ref_id", updatable = false)
	@JsonProperty("b")
	private Long messageRefId;

	@Column(name = "message_direction", nullable = false, updatable = false)
	@Convert(converter = MessageDirectionConverter.class)
	@JsonIgnore
	private MessageDirection messageDirection;

	@Column(name = "receiver_type", nullable = false, updatable = false)
	@Convert(converter = ReceiverTypeConverter.class)
	@JsonProperty("c")
	private ReceiverType receiverType;

	@Column(name = "message_type", nullable = false, updatable = false)
	@Convert(converter = MessageTypeConverter.class)
	@JsonProperty("d")
	private MessageType messageType;

	@Column(name = "message_sub_type", updatable = false)
	@Convert(converter = MessageSubTypeConverter.class)
	@JsonProperty("e")
	private MessageSubType messageSubType;

	@Column(name = "content", updatable = false, length = Integer.MAX_VALUE)
	@JsonProperty("f")
	private String content;

	@Column(name = "attachment", updatable = false, length = Integer.MAX_VALUE)
	@JsonProperty("g")
	private String attachment;

	@Column(name = "message_status", nullable = false)
	@Convert(converter = MessageStatusConverter.class)
	@JsonIgnore
	private MessageStatus messageStatus;

	@Column(name = "wait_status", nullable = false)
	@Convert(converter = WaitStatusConverter.class)
	@JsonIgnore
	private WaitStatus waitStatus;

	@Column(name = "date", nullable = false, updatable = false)
	@JsonIgnore
	private Date date;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "contact_id", nullable = false, updatable = false)
	@JsonIgnore
	private Contact contact;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "owner_id", nullable = false, updatable = false)
	@JsonIgnore
	private Contact owner;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "dgroup_id", updatable = false)
	@JsonIgnore
	private Dgroup dgroup;

	@OneToMany(mappedBy = "message", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JsonIgnore
	private Set<StatusReport> statusReports = new HashSet<StatusReport>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ref_message_id", updatable = false)
	@JsonProperty("h")
	private Message refMessage;

	@Transient
	@JsonProperty("i")
	private Long groupRefId;

	@Transient
	@JsonProperty("j")
	private Long contactRefId;

	@Column(name = "message_code", updatable = false)
	@JsonProperty("k")
	private Integer messageCode;

	@Column(name = "api_flag", updatable = false)
	@JsonIgnore
	private Integer apiFlag;

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

	public Message(Message message) {
		super();
		this.id = message.id;
		this.messageRefId = message.messageRefId;
		this.messageDirection = message.messageDirection;
		this.receiverType = message.receiverType;
		this.messageType = message.messageType;
		this.messageSubType = message.messageSubType;
		this.content = message.content;
		this.attachment = message.attachment;
		this.messageStatus = message.messageStatus;
		this.waitStatus = message.waitStatus;
		this.date = message.date;
		this.contact = message.contact;
		this.owner = message.owner;
		this.dgroup = message.dgroup;
		this.statusReports = message.statusReports;
		this.refMessage = message.refMessage;
		this.groupRefId = message.groupRefId;
		this.contactRefId = message.contactRefId;
		this.messageCode = message.messageCode;
		this.apiFlag = message.apiFlag;
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

	public MessageSubType getMessageSubType() {
		return messageSubType;
	}

	public void setMessageSubType(MessageSubType messageSubType) {
		this.messageSubType = messageSubType;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getAttachment() {
		return attachment;
	}

	public void setAttachment(String attachment) {
		this.attachment = attachment;
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

	public Contact getOwner() {
		return owner;
	}

	public void setOwner(Contact owner) {
		this.owner = owner;
	}

	public Dgroup getDgroup() {
		return dgroup;
	}

	public void setDgroup(Dgroup dgroup) {
		this.dgroup = dgroup;
	}

	public Set<StatusReport> getStatusReports() {
		return statusReports;
	}

	public void addStatusReport(StatusReport statusReport) {
		this.statusReports.add(statusReport);
		statusReport.setMessage(this);
	}

	public void removeStatusReport(StatusReport statusReport) {
		this.statusReports.remove(statusReport);
		statusReport.setMessage(null);
	}

	public Message getRefMessage() {
		return refMessage;
	}

	public void setRefMessage(Message refMessage) {
		this.refMessage = refMessage;
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

	public Integer getMessageCode() {
		return messageCode;
	}

	public void setMessageCode(Integer messageCode) {
		this.messageCode = messageCode;
	}

	public Integer getApiFlag() {
		return apiFlag;
	}

	public void setApiFlag(Integer apiFlag) {
		this.apiFlag = apiFlag;
	}

	public MessageStatus getOverallStatus() {

		if (statusReports.size() == 0)
			return MessageStatus.READ;

		int minOrder = Integer.MAX_VALUE;

		for (StatusReport statusReport : statusReports) {

			minOrder = Math.min(minOrder, statusReport.getMessageStatus().index());

		}

		return MessageStatus.of(minOrder);

	}

	@PrePersist
	protected void onCreate() {
		this.date = new Date();
		if (this.owner == null)
			this.owner = this.contact;
	}

}
