package com.ogya.dms.core.database.tables;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ogya.dms.core.database.converters.AttachmentTypeConverter;
import com.ogya.dms.core.database.converters.MessageStatusConverter;
import com.ogya.dms.core.database.converters.UpdateTypeConverter;
import com.ogya.dms.core.database.converters.ViewStatusConverter;
import com.ogya.dms.core.structures.AttachmentType;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.UpdateType;
import com.ogya.dms.core.structures.ViewStatus;

@Entity
@Table(name = "message")
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_gen")
	@SequenceGenerator(name = "message_gen", sequenceName = "message_seq", initialValue = 1, allocationSize = 1)
	@JsonProperty("a")
	private Long id;

	@Column(name = "message_ref_id", updatable = false)
	@JsonProperty("b")
	private Long messageRefId;

	@Column(name = "content", updatable = false, length = 1000000000)
	@JsonProperty("c")
	private String content;

	@Column(name = "attachment_name", updatable = false, length = 1000000000)
	@JsonProperty("d")
	private String attachmentName;

	@Column(name = "update_type", updatable = false)
	@Convert(converter = UpdateTypeConverter.class)
	@JsonProperty("e")
	private UpdateType updateType;

	@Column(name = "attachment_type", updatable = false)
	@Convert(converter = AttachmentTypeConverter.class)
	@JsonProperty("f")
	private AttachmentType attachmentType;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ref_message_id", updatable = false)
	@JsonProperty("g")
	private Message refMessage;

	@Transient
	@JsonProperty("h")
	private Boolean senderGroupOwner;

	@Transient
	@JsonProperty("i")
	private Long groupRefId;

	@Transient
	@JsonProperty("j")
	private Long contactRefId;

	@Column(name = "message_code", updatable = false)
	@JsonProperty("k")
	private Integer messageCode;

	@Column(name = "forward_count", updatable = false)
	@JsonProperty("l")
	private Integer forwardCount;

	@Column(name = "date", nullable = false, updatable = false)
	@JsonIgnore
	private Date date;

	@Column(name = "attachment_path", length = 1000000000)
	@JsonIgnore
	private String attachmentPath;

	@Column(name = "local", nullable = false, updatable = false)
	@JsonIgnore
	private boolean local = false;

	@Column(name = "done", nullable = false)
	@JsonIgnore
	private boolean done = false;

	@Column(name = "message_status", nullable = false)
	@Convert(converter = MessageStatusConverter.class)
	@JsonIgnore
	private MessageStatus messageStatus;

	@Column(name = "view_status")
	@Convert(converter = ViewStatusConverter.class)
	@JsonIgnore
	private ViewStatus viewStatus;

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
	private final Set<StatusReport> statusReports = new HashSet<StatusReport>();

	@Column(name = "api_flag", updatable = false)
	@JsonIgnore
	private Boolean apiFlag;

	public Message() {
		super();
	}

	public Message(String content, Message refMessage, Integer messageCode, MessageStatus messageStatus,
			Contact contact, Contact owner, Dgroup dgroup, Boolean apiFlag) {
		super();
		this.content = content;
		this.refMessage = refMessage;
		this.messageCode = messageCode;
		this.messageStatus = messageStatus;
		this.contact = contact;
		this.owner = owner;
		this.dgroup = dgroup;
		this.apiFlag = apiFlag;
	}

	public Message(Message message) {
		super();
		this.id = message.id;
		this.messageRefId = message.messageRefId;
		this.content = message.content;
		this.attachmentName = message.attachmentName;
		this.updateType = message.updateType;
		this.attachmentType = message.attachmentType;
		this.refMessage = message.refMessage;
		this.groupRefId = message.groupRefId;
		this.contactRefId = message.contactRefId;
		this.senderGroupOwner = message.senderGroupOwner;
		this.messageCode = message.messageCode;
		this.forwardCount = message.forwardCount;
		this.date = message.date;
		this.attachmentPath = message.attachmentPath;
		this.local = message.local;
		this.done = message.done;
		this.messageStatus = message.messageStatus;
		this.viewStatus = message.viewStatus;
		this.contact = message.contact;
		this.owner = message.owner;
		this.dgroup = message.dgroup;
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

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getAttachmentName() {
		return attachmentName;
	}

	public void setAttachmentName(String attachmentName) {
		this.attachmentName = attachmentName;
	}

	public UpdateType getUpdateType() {
		return updateType;
	}

	public void setUpdateType(UpdateType updateType) {
		this.updateType = updateType;
	}

	public AttachmentType getAttachmentType() {
		return attachmentType;
	}

	public void setAttachmentType(AttachmentType attachmentType) {
		this.attachmentType = attachmentType;
	}

	public Message getRefMessage() {
		return refMessage;
	}

	public void setRefMessage(Message refMessage) {
		this.refMessage = refMessage;
	}

	public Boolean getSenderGroupOwner() {
		return senderGroupOwner;
	}

	public void setSenderGroupOwner(Boolean senderGroupOwner) {
		this.senderGroupOwner = senderGroupOwner;
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

	public Integer getForwardCount() {
		return forwardCount;
	}

	public void setForwardCount(Integer forwardCount) {
		this.forwardCount = forwardCount;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getAttachmentPath() {
		return attachmentPath;
	}

	public void setAttachmentPath(String attachmentPath) {
		this.attachmentPath = attachmentPath;
	}

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public MessageStatus getMessageStatus() {
		return messageStatus;
	}

	public void setMessageStatus(MessageStatus messageStatus) {
		this.messageStatus = messageStatus;
	}

	public ViewStatus getViewStatus() {
		return viewStatus;
	}

	public void setViewStatus(ViewStatus viewStatus) {
		this.viewStatus = viewStatus;
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
		statusReports.add(statusReport);
		statusReport.setMessage(this);
	}

	public void removeStatusReport(StatusReport statusReport) {
		statusReports.remove(statusReport);
		statusReport.setMessage(null);
	}

	public Boolean getApiFlag() {
		return apiFlag;
	}

	public void setApiFlag(Boolean apiFlag) {
		this.apiFlag = apiFlag;
	}

	public EntityBase getEntity() {
		return dgroup == null ? contact : dgroup;
	}

	public MessageStatus getOverallStatus() {

		if (statusReports.size() == 0) {
			return MessageStatus.READ;
		}

		int minOrder = Integer.MAX_VALUE;

		for (StatusReport statusReport : statusReports) {

			minOrder = Math.min(minOrder, statusReport.getMessageStatus().index());

		}

		return MessageStatus.of(minOrder);

	}

	@PrePersist
	protected void prePersist() {
		date = new Date();
		if (updateType == null && viewStatus == null) {
			viewStatus = ViewStatus.DEFAULT;
		}
		if (owner == null) {
			owner = contact;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Message)) {
			return false;
		}
		Message message = (Message) obj;
		return Objects.equals(this.id, message.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

}
