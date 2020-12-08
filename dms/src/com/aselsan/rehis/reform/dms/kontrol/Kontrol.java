package com.aselsan.rehis.reform.dms.kontrol;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import com.aselsan.rehis.reform.dms.arayuz.DmsKontrol;
import com.aselsan.rehis.reform.dms.arayuz.dinleyici.DmsDinleyici;
import com.aselsan.rehis.reform.dms.arayuz.dinleyici.DmsGuiDinleyici;
import com.aselsan.rehis.reform.dms.arayuz.hata.VeritabaniHatasi;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsGrup;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsGrupSecim;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsKisi;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsKisiSecim;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme.DmsAktifGruplarGercekleme;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme.DmsCevrimiciKisilerGercekleme;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme.DmsDosyaGercekleme;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme.DmsGrupGercekleme;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme.DmsKisiGercekleme;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme.DmsListeGercekleme;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme.DmsMesajGercekleme;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.gercekleme.DmsNesneGercekleme;
import com.aselsan.rehis.reform.dms.arayuz.veriyapisi.DmsDurum;
import com.ogya.dms.intf.DmsHandle;
import com.ogya.dms.intf.exceptions.DbException;
import com.ogya.dms.intf.handles.ContactHandle;
import com.ogya.dms.intf.handles.FileHandle;
import com.ogya.dms.intf.handles.GroupHandle;
import com.ogya.dms.intf.handles.ListHandle;
import com.ogya.dms.intf.handles.MessageHandle;
import com.ogya.dms.intf.handles.ObjectHandle;
import com.ogya.dms.intf.listeners.DmsGuiListener;
import com.ogya.dms.intf.listeners.DmsListener;
import com.ogya.dms.main.DmsCore;
import com.ogya.dms.structures.Availability;

public class Kontrol implements DmsKontrol {

	private static final Map<String, Kontrol> INSTANCES = Collections.synchronizedMap(new HashMap<String, Kontrol>());

	private final DmsHandle dmsHandle;

	private final Map<DmsDinleyici, DmsListener> dinleyiciMap = new HashMap<DmsDinleyici, DmsListener>();
	private final Map<DmsGuiDinleyici, DmsGuiListener> guiDinleyiciMap = new HashMap<DmsGuiDinleyici, DmsGuiListener>();

	private Kontrol(DmsHandle dmsHandle) {

		this.dmsHandle = dmsHandle;

	}

	public static DmsKontrol getInstance(String kullaniciAdi, String sifre) throws VeritabaniHatasi {

		try {

			INSTANCES.putIfAbsent(kullaniciAdi, new Kontrol(DmsCore.login(kullaniciAdi, sifre)));

		} catch (DbException e) {

			throw new VeritabaniHatasi(e.getMessage());

		}

		return INSTANCES.get(kullaniciAdi);

	}

	@Override
	public JComponent getDmsPanel() {
		return dmsHandle.getDmsPanel();
	}

	@Override
	public void dinleyiciEkle(DmsDinleyici dmsDinleyici) {

		DmsListener dmsListener = new DmsListener() {

			@Override
			public void objectReceived(ObjectHandle arg0) {
				dmsDinleyici.nesneAlindi(new DmsNesneGercekleme(arg0));
			}

			@Override
			public void messageReceived(MessageHandle arg0) {
				dmsDinleyici.mesajAlindi(new DmsMesajGercekleme(arg0));
			}

			@Override
			public void listReceived(ListHandle arg0) {
				dmsDinleyici.listeAlindi(new DmsListeGercekleme(arg0));
			}

			@Override
			public void fileReceived(FileHandle arg0) {
				dmsDinleyici.dosyaAlindi(new DmsDosyaGercekleme(arg0));
			}

			@Override
			public void fileClicked(Path arg0) {
				dmsDinleyici.dosyaTiklandi(arg0);
			}

			@Override
			public void contactUpdated(ContactHandle arg0) {
				dmsDinleyici.kisiGuncellendi(new DmsKisiGercekleme(arg0));
			}

			@Override
			public void groupUpdated(GroupHandle arg0) {
				dmsDinleyici.grupGuncellendi(new DmsGrupGercekleme(arg0));
			}

		};

		dinleyiciMap.put(dmsDinleyici, dmsListener);

		dmsHandle.addListener(dmsListener);

	}

	@Override
	public void dinleyiciCikar(DmsDinleyici dmsDinleyici) {
		dmsHandle.removeListener(dinleyiciMap.remove(dmsDinleyici));
	}

	@Override
	public void guiDinleyiciEkle(DmsGuiDinleyici dmsGuiDinleyici) {

		DmsGuiListener dmsGuiListener = new DmsGuiListener() {

			@Override
			public void guiAudioReceived(FileHandle arg0) {
				dmsGuiDinleyici.guiSesKaydiAlindi(new DmsDosyaGercekleme(arg0));
			}

			@Override
			public void guiAudioSent(FileHandle arg0) {
				dmsGuiDinleyici.guiSesKaydiGonderildi(new DmsDosyaGercekleme(arg0));
			}

			@Override
			public void guiFileReceived(FileHandle arg0) {
				dmsGuiDinleyici.guiDosyaAlindi(new DmsDosyaGercekleme(arg0));
			}

			@Override
			public void guiFileSent(FileHandle arg0) {
				dmsGuiDinleyici.guiDosyaGonderildi(new DmsDosyaGercekleme(arg0));
			}

			@Override
			public void guiMessageReceived(MessageHandle arg0) {
				dmsGuiDinleyici.guiMesajAlindi(new DmsMesajGercekleme(arg0));
			}

			@Override
			public void guiMessageSent(MessageHandle arg0) {
				dmsGuiDinleyici.guiMesajGonderildi(new DmsMesajGercekleme(arg0));
			}

			@Override
			public void guiReportReceived(FileHandle arg0) {
				dmsGuiDinleyici.guiRaporAlindi(new DmsDosyaGercekleme(arg0));
			}

			@Override
			public void guiReportSent(FileHandle arg0) {
				dmsGuiDinleyici.guiRaporGonderildi(new DmsDosyaGercekleme(arg0));
			}

		};

		guiDinleyiciMap.put(dmsGuiDinleyici, dmsGuiListener);

		dmsHandle.addGuiListener(dmsGuiListener);

	}

	@Override
	public void guiDinleyiciCikar(DmsGuiDinleyici dmsGuiDinleyici) {
		dmsHandle.removeGuiListener(guiDinleyiciMap.remove(dmsGuiDinleyici));
	}

	@Override
	public void setKoordinatlar(Double enlem, Double boylam) {
		dmsHandle.setCoordinates(enlem, boylam);
	}

	@Override
	public void setAciklama(String aciklama) {
		dmsHandle.setComment(aciklama);
	}

	@Override
	public void setDurum(DmsDurum dmsDurum) {
		dmsHandle.setAvailability(Availability.values()[dmsDurum.ordinal()]);
	}

	@Override
	public DmsKisi getDmsKisim() {
		return new DmsKisiGercekleme(dmsHandle.getMyContactHandle());
	}

	@Override
	public DmsGrupSecim getDmsAktifGruplar() {
		return new DmsAktifGruplarGercekleme(dmsHandle.getActiveGroupsHandle());
	}

	@Override
	public DmsKisiSecim getDmsCevrimiciKisiler() {
		return new DmsCevrimiciKisilerGercekleme(dmsHandle.getOnlineContactsHandle());
	}

	@Override
	public DmsKisi getDmsKisi(Long kisiId) {
		return new DmsKisiGercekleme(dmsHandle.getContactHandle(kisiId));
	}

	@Override
	public DmsGrup getDmsGrup(Long grupId) {
		return new DmsGrupGercekleme(dmsHandle.getGroupHandle(grupId));
	}

	@Override
	public List<DmsKisi> getTumDmsKisiler() {
		List<DmsKisi> tumDmsKisiler = new ArrayList<DmsKisi>();
		dmsHandle.getAllContactHandles()
				.forEach(contactHandle -> tumDmsKisiler.add(new DmsKisiGercekleme(contactHandle)));
		return tumDmsKisiler;
	}

	@Override
	public List<DmsGrup> getTumDmsGruplar() {
		List<DmsGrup> tumDmsGruplar = new ArrayList<DmsGrup>();
		dmsHandle.getAllGroupHandles().forEach(groupHandle -> tumDmsGruplar.add(new DmsGrupGercekleme(groupHandle)));
		return tumDmsGruplar;
	}

	@Override
	public List<Long> getAdrestekiIdler(InetAddress adres) {
		return dmsHandle.getIdsByAddress(adres);
	}

	@Override
	public boolean kisilereMesajGonder(String mesaj, Integer mesajKodu, List<Long> kisiIdler) {
		return dmsHandle.sendMessageToContacts(mesaj, mesajKodu, kisiIdler);
	}

	@Override
	public boolean grubaMesajGonder(String mesaj, Integer mesajKodu, Long grupId) {
		return dmsHandle.sendMessageToGroup(mesaj, mesajKodu, grupId);
	}

	@Override
	public boolean kisilereNesneGonder(Object nesne, Integer nesneKodu, List<Long> kisiIdler) {
		return dmsHandle.sendObjectToContacts(nesne, nesneKodu, kisiIdler);
	}

	@Override
	public boolean grubaNesneGonder(Object nesne, Integer nesneKodu, Long grupId) {
		return dmsHandle.sendObjectToGroup(nesne, nesneKodu, grupId);
	}

	@Override
	public <T> boolean kisilereListeGonder(List<T> liste, Class<T> elemanTipi, Integer listeKodu,
			List<Long> kisiIdler) {
		return dmsHandle.sendListToContacts(liste, elemanTipi, listeKodu, kisiIdler);
	}

	@Override
	public <T> boolean grubaListeGonder(List<T> liste, Class<T> elemanTipi, Integer listeKodu, Long grupId) {
		return dmsHandle.sendListToGroup(liste, elemanTipi, listeKodu, grupId);
	}

	@Override
	public boolean kisilereDosyaGonder(Path dosyaYolu, Integer dosyaKodu, List<Long> kisiIdler) {
		return dmsHandle.sendFileToContacts(dosyaYolu, dosyaKodu, kisiIdler);
	}

	@Override
	public boolean grubaDosyaGonder(Path dosyaYolu, Integer dosyaKodu, Long grupId) {
		return dmsHandle.sendFileToGroup(dosyaYolu, dosyaKodu, grupId);
	}

}
