package com.aselsan.rehis.reform.mcsy.veritabani.tablolar;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import com.aselsan.rehis.reform.mcsy.ortak.OrtakSabitler;

@Entity
@Table(name = "kimlik")
public class Kimlik {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "uuid", unique = true, nullable = false, updatable = false)
	private String uuid;

	@Column(name = "isim", unique = true, nullable = false, updatable = false)
	private String isim;

	@Column(name = "aciklama")
	private String aciklama;

	@Column(name = "durum", nullable = false)
	private Integer durum;

	@Column(name = "enlem")
	private Double enlem;

	@Column(name = "boylam")
	private Double boylam;

	public Kimlik() {

	}

	public Kimlik(String isim) {

		this.isim = isim;

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

	public Integer getDurum() {
		return durum;
	}

	public void setDurum(Integer durum) {
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

	@PrePersist
	private void onCreate() {

		this.uuid = UUID.randomUUID().toString();

		this.durum = OrtakSabitler.DURUM_MUSAIT;

	}

}
