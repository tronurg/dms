package com.aselsan.rehis.reform.mcsy.veritabani.tablolar;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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

	@Column(name = "uuid_kurucu")
	private String uuidKurucu;

	@ManyToMany(mappedBy = "gruplar")
	private Set<Kisi> kisiler = new HashSet<Kisi>();

	@PrePersist
	private void onCreate() {

		if (uuid == null)
			uuid = UUID.randomUUID().toString();

	}

}
