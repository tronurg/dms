package com.aselsan.rehis.reform.mcsy.veritabani.tablolar;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "grup")
public class Grup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "uuid", nullable = false, updatable = false)
	private String uuid;

	@Column(name = "isim", nullable = false, updatable = false)
	private String isim;

	@Column(name = "aciklama")
	private String aciklama;

	@Column(name = "uuid_kurucu", nullable = false, updatable = false)
	private String uuidKurucu;

	@ManyToMany
	@JoinTable(name = "grup_kisiler", joinColumns = { @JoinColumn(name = "grup_id") }, inverseJoinColumns = {
			@JoinColumn(name = "kisi_id") })
	private Set<Kisi> kisiler = new HashSet<Kisi>();

	public Grup() {
		super();
	}

	public Grup(Long id, String uuid, String isim, String aciklama, String uuidKurucu, Set<Kisi> kisiler) {
		this.isim = isim;
		this.uuidKurucu = uuidKurucu;
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

	public String getIsim() {
		return isim;
	}

	public void setIsim(String isim) {
		this.isim = isim;
	}

	public String getAciklama() {
		return aciklama;
	}

	public void setAciklama(String aciklama) {
		this.aciklama = aciklama;
	}

	public String getUuidKurucu() {
		return uuidKurucu;
	}

	public void setUuidKurucu(String uuidKurucu) {
		this.uuidKurucu = uuidKurucu;
	}

	public Set<Kisi> getKisiler() {
		return kisiler;
	}

	public void setKisiler(Set<Kisi> kisiler) {
		this.kisiler = kisiler;
	}

	@PrePersist
	private void onCreate() {
		this.uuid = UUID.randomUUID().toString();
	}

}
