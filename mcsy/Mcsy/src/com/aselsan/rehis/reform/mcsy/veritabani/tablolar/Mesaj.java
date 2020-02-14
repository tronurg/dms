package com.aselsan.rehis.reform.mcsy.veritabani.tablolar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "mesaj")
public class Mesaj {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "gonderen_uuid")
	private String gonderenUuid;

	@Column(name = "alici_uuid")
	private String aliciUuid;

	@Column(name = "ozel_mesaj_kodu")
	private Integer ozelMesajKodu;

	@Column(name = "acik_mesaj_kodu")
	private Integer acikMesajKodu;

	@Column(name = "icerik")
	private String icerik;

	@Column(name = "gonderildi")
	private Boolean gonderildi;

}
