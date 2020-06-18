package com.ogya.dms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ogya.dms.database.tables.Group;
import com.ogya.dms.database.tables.Identity;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.structures.ContactStatus;

public class Model {

	private final Identity kimlik;

	private final AtomicBoolean sunucuBagli = new AtomicBoolean(false);

	private final Map<String, Contact> kisiler = Collections.synchronizedMap(new HashMap<String, Contact>());
	private final Map<String, Group> gruplar = Collections.synchronizedMap(new HashMap<String, Group>());

	private final List<String> acikUuidler = Collections.synchronizedList(new ArrayList<String>());

	private final Map<String, Long> minMesajIdler = Collections.synchronizedMap(new HashMap<String, Long>());

	public Model(Identity kimlik) {

		this.kimlik = kimlik;

	}

	public Identity getKimlik() {

		return kimlik;

	}

	public void aciklamaGuncelle(String aciklama) {

		kimlik.setAciklama(aciklama);

	}

	public void durumGuncelle(ContactStatus durum) {

		kimlik.setDurum(durum);

	}

	public boolean isSunucuBagli() {

		return sunucuBagli.get();

	}

	public void setSunucuBaglantiDurumu(boolean arg0) {

		sunucuBagli.set(arg0);

	}

	public void addKisi(Contact kisi) {

		kisiler.put(kisi.getUuid(), kisi);

	}

	public Contact getKisi(String uuid) {

		return kisiler.get(uuid);

	}

	public boolean isKisiCevrimici(String uuid) {

		return kisiler.containsKey(uuid) && !getKisi(uuid).getDurum().equals(ContactStatus.CEVRIMDISI);

	}

	public Map<String, Contact> getKisiler() {

		return kisiler;

	}

	public void addGrup(Group grup) {

		gruplar.put(grup.getUuid(), grup);

	}

	public Group getGrup(String uuid) {

		return gruplar.get(uuid);

	}

	public Map<String, Group> getGruplar() {

		return gruplar;

	}

	public void mesajPaneliAcildi(String uuid) {

		acikUuidler.add(uuid);

	}

	public void mesajPaneliKapandi(String uuid) {

		acikUuidler.remove(uuid);

	}

	public boolean isMesajPaneliAcik(String uuid) {

		return acikUuidler.contains(uuid);

	}

	public void mesajIdEkle(String uuid, Long id) {

		if (minMesajIdler.containsKey(uuid) && minMesajIdler.get(uuid) < id)
			return;

		minMesajIdler.put(uuid, id);

	}

	public Long getMinMesajId(String uuid) {

		if (!minMesajIdler.containsKey(uuid))
			return -1L;

		return minMesajIdler.get(uuid);

	}

}
