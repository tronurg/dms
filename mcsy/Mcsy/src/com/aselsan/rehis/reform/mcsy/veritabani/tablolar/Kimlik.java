package com.aselsan.rehis.reform.mcsy.veritabani.tablolar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "kimlik")
public class Kimlik {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "uuid", nullable = false, updatable = false)
	private String uuid;

	@Column(name = "isim")
	private String isim;

	@Column(name = "aciklama")
	private String aciklama;

	@Column(name = "durum")
	private Integer durum;

	@Column(name = "enlem")
	private Double enlem;

	@Column(name = "boylam")
	private Double boylam;

}
