package com.aselsan.rehis.reform.mcsy.veritabani.tablolar;

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

import com.aselsan.rehis.reform.mcsy.mcistemci.veriyapilari.MesajTipi;
import com.aselsan.rehis.reform.mcsy.veriyapilari.MesajDurumu;

@Entity
@Table(name = "mesaj")
public class Mesaj {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "mesaj_id")
	private Long mesajId;

	@Column(name = "gonderen_uuid", nullable = false, updatable = false)
	private String gonderenUuid;

	@Column(name = "alici_uuid", nullable = false, updatable = false)
	private String aliciUuid;

	@Column(name = "mesaj_tipi", nullable = false, updatable = false)
	@Enumerated(EnumType.STRING)
	private MesajTipi mesajTipi;

	@Column(name = "mesaj_kodu", updatable = false)
	private Integer mesajKodu;

	@Column(name = "icerik", nullable = false, updatable = false)
	private String icerik;

	@Column(name = "mesaj_durumu", nullable = false)
	@Enumerated(EnumType.STRING)
	private MesajDurumu mesajDurumu;

	@Column(name = "tarih", nullable = false, updatable = false)
	private Date tarih;

	public Mesaj() {
		super();
	}

	public Mesaj(String gonderenUuid, String aliciUuid, MesajTipi mesajTipi, String icerik) {
		this.gonderenUuid = gonderenUuid;
		this.aliciUuid = aliciUuid;
		this.mesajTipi = mesajTipi;
		this.icerik = icerik;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMesajId() {
		return mesajId;
	}

	public void setMesajId(Long mesajId) {
		this.mesajId = mesajId;
	}

	public String getGonderenUuid() {
		return gonderenUuid;
	}

	public void setGonderenUuid(String gonderenUuid) {
		this.gonderenUuid = gonderenUuid;
	}

	public String getAliciUuid() {
		return aliciUuid;
	}

	public void setAliciUuid(String aliciUuid) {
		this.aliciUuid = aliciUuid;
	}

	public MesajTipi getMesajTipi() {
		return mesajTipi;
	}

	public void setMesajTipi(MesajTipi mesajTipi) {
		this.mesajTipi = mesajTipi;
	}

	public Integer getMesajKodu() {
		return mesajKodu;
	}

	public void setMesajKodu(Integer mesajKodu) {
		this.mesajKodu = mesajKodu;
	}

	public String getIcerik() {
		return icerik;
	}

	public void setIcerik(String icerik) {
		this.icerik = icerik;
	}

	public MesajDurumu getMesajDurumu() {
		return mesajDurumu;
	}

	public void setMesajDurumu(MesajDurumu mesajDurumu) {
		this.mesajDurumu = mesajDurumu;
	}

	public Date getTarih() {
		return tarih;
	}

	public void setTarih(Date tarih) {
		this.tarih = tarih;
	}

	@PrePersist
	protected void onCreate() {
		this.tarih = new Date();
	}

	@PostPersist
	protected void onPersist() {
		if (this.mesajId != null)
			return;
		this.mesajId = this.id;
	}

}
