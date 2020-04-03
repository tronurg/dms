package com.aselsan.rehis.reform.mcsy.veritabani.tablolar;

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

import com.aselsan.rehis.reform.mcsy.veriyapilari.KisiDurumu;

@Entity
@Table(name = "kisi")
public class Kisi {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "uuid", unique = true, nullable = false, updatable = false)
	private String uuid;

	@Column(name = "isim", nullable = false, updatable = false)
	private String isim;

	@Column(name = "aciklama")
	private String aciklama;

	@Column(name = "durum", nullable = false)
	@Enumerated(EnumType.STRING)
	private KisiDurumu durum;

	@Column(name = "enlem")
	private Double enlem;

	@Column(name = "boylam")
	private Double boylam;

	@ManyToMany(mappedBy = "kisiler")
	private Set<Grup> gruplar = new HashSet<Grup>();

	public Kisi() {
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

	public KisiDurumu getDurum() {
		return durum;
	}

	public void setDurum(KisiDurumu durum) {
		this.durum = durum;
	}

	public Double getEnlem() {
		return enlem;
	}

	public void setEnlem(Double enlem) {
		this.enlem = enlem;
	}

	public Double getBoylam() {
		return boylam;
	}

	public void setBoylam(Double boylam) {
		this.boylam = boylam;
	}

	public Set<Grup> getGruplar() {
		return gruplar;
	}

	public void setGruplar(Set<Grup> gruplar) {
		this.gruplar = gruplar;
	}

}
