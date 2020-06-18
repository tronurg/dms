package com.ogya.dms.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.JComponent;

import org.hibernate.HibernateException;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.ogya.dms.common.CommonConstants;
import com.ogya.dms.database.DbManager;
import com.ogya.dms.database.tables.Group;
import com.ogya.dms.database.tables.Identity;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.dmsclient.DmsClient;
import com.ogya.dms.dmsclient.intf.DmsClientListener;
import com.ogya.dms.intf.DmsHandle;
import com.ogya.dms.intf.exceptions.DbException;
import com.ogya.dms.model.Model;
import com.ogya.dms.structures.ContactStatus;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageType;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.view.DmsPanel;
import com.ogya.dms.view.intf.AppListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class Control implements AppListener, DmsClientListener, DmsHandle {

	private static final Map<String, Control> INSTANCES = Collections.synchronizedMap(new HashMap<String, Control>());

	private static final int SAYFA_MIN_MESAJ_SAYISI = 50;

	private final DbManager veritabaniYonetici;

	private final Model model;

	private final DmsPanel mcPanel;
	private final JFXPanel mcPanelSwing;

	private final DmsClient mcIstemci;

	private final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

		@Override
		public boolean shouldSkipField(FieldAttributes arg0) {
			return arg0.getName().equals("id");
		}

		@Override
		public boolean shouldSkipClass(Class<?> arg0) {
			return false;
		}

	}).create();

	private final Object beaconSyncObj = new Object();

	private final ExecutorService islemKuyrugu = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {
			Thread thread = new Thread(arg0);
			thread.setDaemon(true);
			return thread;
		}

	});

	private Control(String kullaniciAdi, String kullaniciSifresi) throws DbException {

		veritabaniYonetici = new DbManager(kullaniciAdi, kullaniciSifresi);

		Identity kimlik = veritabaniYonetici.getKimlik();

		model = new Model(kimlik);

		mcPanelSwing = new JFXPanel();
		mcPanel = new DmsPanel();

		mcPanel.dinleyiciEkle(this);

		initVeritabani();
		initModel();
		initGUI();

		mcIstemci = new DmsClient(kimlik.getUuid(), CommonConstants.SUNUCU_IP, CommonConstants.SUNUCU_PORT, this);

		ilklendir();

	}

	public static Control getInstance(String kullaniciAdi, String kullaniciSifresi) throws DbException {

		INSTANCES.putIfAbsent(kullaniciAdi, new Control(kullaniciAdi, kullaniciSifresi));

		return INSTANCES.get(kullaniciAdi);

	}

	private void ilklendir() {

		Thread beaconYayinlaThread = new Thread(this::beaconYayinla);
		beaconYayinlaThread.setDaemon(true);
		beaconYayinlaThread.start();

	}

	private void initVeritabani() {

		veritabaniYonetici.tumKisileriAl().forEach(kisi -> {
			kisi.setDurum(ContactStatus.CEVRIMDISI);
			veritabaniYonetici.kisiEkleGuncelle(kisi);
		});

	}

	private void initModel() {

		veritabaniYonetici.tumKisileriAl().forEach(kisi -> model.addKisi(kisi));
		veritabaniYonetici.tumGruplariAl().forEach(grup -> model.addGrup(grup));

	}

	private void initGUI() {

		Platform.runLater(() -> {
			mcPanelSwing.setScene(new Scene(mcPanel));
			mcPanel.setKimlik(model.getKimlik());
		});

		model.getKisiler().forEach((uuid, kisi) -> Platform.runLater(() -> mcPanel.kisiGuncelle(kisi)));

		model.getGruplar().forEach((uuid, grup) -> Platform.runLater(() -> mcPanel.grupGuncelle(grup)));

		try {

			String yerelUuid = model.getKimlik().getUuid();

			Set<String> karsiUuidler = veritabaniYonetici.getUuidIleMesajlasanTumUuidler(yerelUuid);

			karsiUuidler.forEach(karsiUuid -> {

				List<Message> vtMesajlar = new ArrayList<Message>();

				vtMesajlar.addAll(veritabaniYonetici.getIlkOkunmamisMesajdanItibarenTumMesajlar(yerelUuid, karsiUuid));

				if (vtMesajlar.size() == 0) {

					vtMesajlar.addAll(veritabaniYonetici.getSonMesajlar(yerelUuid, karsiUuid, SAYFA_MIN_MESAJ_SAYISI));

				} else if (vtMesajlar.size() < SAYFA_MIN_MESAJ_SAYISI) {

					vtMesajlar.addAll(veritabaniYonetici.getIddenOncekiSonMesajlar(yerelUuid, karsiUuid,
							vtMesajlar.get(0).getId(), SAYFA_MIN_MESAJ_SAYISI - vtMesajlar.size()));

				}

				vtMesajlar.forEach(mesaj -> paneleMesajEkle(mesaj));

			});

		} catch (HibernateException e) {

			e.printStackTrace();

		}

	}

	private void paneleMesajEkle(Message mesaj) {

		String yerelUuid = model.getKimlik().getUuid();

		if (yerelUuid.equals(mesaj.getGonderenUuid())) {

			final String karsiUuid = mesaj.getAliciUuid();

			model.mesajIdEkle(karsiUuid, mesaj.getId());

			Platform.runLater(() -> mcPanel.mesajEkle(mesaj, MessageDirection.GIDEN, karsiUuid));

		} else if (yerelUuid.equals(mesaj.getAliciUuid())) {

			final String karsiUuid = mesaj.getGonderenUuid();

			model.mesajIdEkle(karsiUuid, mesaj.getId());

			Platform.runLater(() -> mcPanel.mesajEkle(mesaj, MessageDirection.GELEN, karsiUuid));

		}

	}

	private void beaconYayinla() {

		while (true) {

			synchronized (beaconSyncObj) {

				if (model.isSunucuBagli())
					mcIstemci.beaconGonder(gson.toJson(model.getKimlik()));

				try {

					beaconSyncObj.wait(CommonConstants.BEACON_ARALIK_MS);

				} catch (InterruptedException e) {

				}

			}

		}

	}

	private void kisiKoptu(Contact kisi) {

		islemKuyrugu.execute(() -> {

			kisi.setDurum(ContactStatus.CEVRIMDISI);

			try {

				final Contact yeniKisi = veritabaniYonetici.kisiEkleGuncelle(kisi);

				model.addKisi(yeniKisi);

				Platform.runLater(() -> mcPanel.kisiGuncelle(yeniKisi));

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	private Message gidenMesajOlustur(String mesaj, String aliciUuid) throws Exception {

		Message gidenMesaj = new Message(model.getKimlik().getUuid(), aliciUuid, MessageType.MESAJ, mesaj);

		gidenMesaj.setMesajDurumu(MessageStatus.OLUSTURULDU);

		Message yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(gidenMesaj);

		return yeniMesaj;

	}

	private Message gelenMesajOlustur(String mesaj) throws Exception {

		Message gelenMesaj = gson.fromJson(mesaj, Message.class);

		if (model.isMesajPaneliAcik(gelenMesaj.getGonderenUuid())) {

			gelenMesaj.setMesajDurumu(MessageStatus.OKUNDU);

		} else {

			gelenMesaj.setMesajDurumu(MessageStatus.ULASTI);

		}

		Message yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(gelenMesaj);

		return yeniMesaj;

	}

	private Message mesajGonder(Message mesaj) {

		String aliciUuid = mesaj.getAliciUuid();

		if (!model.isKisiCevrimici(aliciUuid))
			return mesaj;

		mcIstemci.mesajGonder(gson.toJson(mesaj), aliciUuid);

		try {

			mesaj.setMesajDurumu(MessageStatus.GONDERILDI);

			Message yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(mesaj);

			return yeniMesaj;

		} catch (HibernateException e) {

			e.printStackTrace();

		}

		return mesaj;

	}

	@Override
	public void beaconAlindi(String mesaj) {

		islemKuyrugu.execute(() -> {

			try {

				Contact gelenKisi = gson.fromJson(mesaj, Contact.class);

				final String uuid = gelenKisi.getUuid();
				boolean wasCevrimici = model.isKisiCevrimici(uuid);

				final Contact yeniKisi = veritabaniYonetici.kisiEkleGuncelle(gelenKisi);

				model.addKisi(yeniKisi);

				Platform.runLater(() -> mcPanel.kisiGuncelle(yeniKisi));

				if (!wasCevrimici) {
					// Simdi cevrimici olduysa bekleyen mesajlarini gonder

					islemKuyrugu.execute(() -> {

						try {

							for (final Message bekleyenMesaj : veritabaniYonetici.getKisiyeGidenBekleyenMesajlar(uuid)) {

								switch (bekleyenMesaj.getMesajDurumu()) {

								case OLUSTURULDU:

									final Message yeniMesaj = mesajGonder(bekleyenMesaj);

									if (!yeniMesaj.getMesajDurumu().equals(MessageStatus.GONDERILDI))
										break;

									Platform.runLater(() -> mcPanel.mesajGuncelle(yeniMesaj, uuid));

									break;

								case GONDERILDI:
								case ULASTI:

									mcIstemci.mesajDurumuIste(Long.toString(bekleyenMesaj.getMesajId()),
											bekleyenMesaj.getAliciUuid());

									break;

								default:

									break;

								}

							}

						} catch (HibernateException e) {

							e.printStackTrace();

						}

					});

				}

			} catch (JsonSyntaxException | HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void mesajAlindi(final String mesaj) {

		islemKuyrugu.execute(() -> {

			try {

				final Message yeniMesaj = gelenMesajOlustur(mesaj);

				paneleMesajEkle(yeniMesaj);

				if (yeniMesaj.getMesajDurumu().equals(MessageStatus.ULASTI))
					mcIstemci.alindiGonder(Long.toString(yeniMesaj.getMesajId()), yeniMesaj.getGonderenUuid());
				else if (yeniMesaj.getMesajDurumu().equals(MessageStatus.OKUNDU))
					mcIstemci.okunduGonder(Long.toString(yeniMesaj.getMesajId()), yeniMesaj.getGonderenUuid());

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void kullaniciKoptu(String uuid) {

		Contact kisi = model.getKisi(uuid);

		if (kisi == null)
			return;

		kisiKoptu(kisi);

	}

	@Override
	public void sunucuBaglantiDurumuGuncellendi(boolean arg0) {

		model.setSunucuBaglantiDurumu(arg0);

		if (arg0) {

			synchronized (beaconSyncObj) {

				beaconSyncObj.notify();

			}

			mcIstemci.tumBeaconlariIste();

		} else {

			model.getKisiler().forEach((uuid, kisi) -> {

				kisiKoptu(kisi);

			});

		}

	}

	@Override
	public void mesajDurumuIstendi(String mesaj, String karsiTarafUuid) {

		final Long mesajId = Long.parseLong(mesaj);

		islemKuyrugu.execute(() -> {

			try {

				Message gelenMesaj = veritabaniYonetici.getMesaj(karsiTarafUuid, mesajId);

				if (gelenMesaj == null) {

					mcIstemci.alinmadiGonder(mesaj, karsiTarafUuid);

				} else if (gelenMesaj.getMesajDurumu().equals(MessageStatus.ULASTI)) {

					mcIstemci.alindiGonder(mesaj, karsiTarafUuid);

				} else if (gelenMesaj.getMesajDurumu().equals(MessageStatus.OKUNDU)) {

					mcIstemci.okunduGonder(mesaj, karsiTarafUuid);

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void karsiTarafMesajiAlmadi(String mesaj, String karsiTarafUuid) {

		final Long mesajId = Long.parseLong(mesaj);

		islemKuyrugu.execute(() -> {

			try {

				Message bekleyenMesaj = veritabaniYonetici.mesajDurumGuncelle(model.getKimlik().getUuid(), mesajId,
						MessageStatus.OLUSTURULDU);

				if (bekleyenMesaj == null)
					return;

				// Mesaji tekrar gonder

				final Message yeniMesaj = mesajGonder(bekleyenMesaj);

				Platform.runLater(() -> mcPanel.mesajGuncelle(yeniMesaj, karsiTarafUuid));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void karsiTarafMesajiAldi(String mesaj, String karsiTarafUuid) {

		final Long mesajId = Long.parseLong(mesaj);

		islemKuyrugu.execute(() -> {

			try {

				final Message gidenMesaj = veritabaniYonetici.mesajDurumGuncelle(model.getKimlik().getUuid(), mesajId,
						MessageStatus.ULASTI);

				if (gidenMesaj == null)
					return;

				Platform.runLater(() -> mcPanel.mesajGuncelle(gidenMesaj, karsiTarafUuid));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void karsiTarafMesajiOkudu(String mesaj, String karsiTarafUuid) {

		final Long mesajId = Long.parseLong(mesaj);

		islemKuyrugu.execute(() -> {

			try {

				final Message gidenMesaj = veritabaniYonetici.mesajDurumGuncelle(model.getKimlik().getUuid(), mesajId,
						MessageStatus.OKUNDU);

				if (gidenMesaj == null)
					return;

				Platform.runLater(() -> mcPanel.mesajGuncelle(gidenMesaj, karsiTarafUuid));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public JComponent getMcPanel() {

		return mcPanelSwing;

	}

	@Override
	public void aciklamaGuncellendi(String aciklama) {

		islemKuyrugu.execute(() -> {

			try {

				Identity kimlik = model.getKimlik();

				if (aciklama.equals(kimlik.getAciklama()))
					return;

				kimlik.setAciklama(aciklama);

				Identity yeniKimlik = veritabaniYonetici.kimlikGuncelle(kimlik);

				model.aciklamaGuncelle(aciklama);

				Platform.runLater(() -> mcPanel.setKimlik(yeniKimlik));

				synchronized (beaconSyncObj) {

					beaconSyncObj.notify();

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void durumGuncelleTiklandi() {

		islemKuyrugu.execute(() -> {

			try {

				Identity kimlik = model.getKimlik();

				if (kimlik.getDurum().equals(ContactStatus.MUSAIT)) {

					kimlik.setDurum(ContactStatus.MESGUL);

				} else if (kimlik.getDurum().equals(ContactStatus.MESGUL)) {

					kimlik.setDurum(ContactStatus.MUSAIT);

				}

				Identity yeniKimlik = veritabaniYonetici.kimlikGuncelle(kimlik);

				model.durumGuncelle(yeniKimlik.getDurum());

				Platform.runLater(() -> mcPanel.setKimlik(yeniKimlik));

				synchronized (beaconSyncObj) {

					beaconSyncObj.notify();

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void kisiMesajPaneliAcildi(final String uuid) {

		islemKuyrugu.execute(() -> {

			model.mesajPaneliAcildi(uuid);

			try {

				List<Message> kisidenGelenBekleyenMesajlar = veritabaniYonetici.getKisidenGelenBekleyenMesajlar(uuid);

				for (final Message gelenMesaj : kisidenGelenBekleyenMesajlar) {

					try {

						gelenMesaj.setMesajDurumu(MessageStatus.OKUNDU);

						final Message yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(gelenMesaj);

						Platform.runLater(() -> mcPanel.mesajGuncelle(yeniMesaj, uuid));

						mcIstemci.okunduGonder(Long.toString(yeniMesaj.getMesajId()), yeniMesaj.getGonderenUuid());

					} catch (JsonSyntaxException | HibernateException e) {

						e.printStackTrace();

					}

				}

				Platform.runLater(() -> mcPanel.ekraniMesajaKaydir(uuid,
						kisidenGelenBekleyenMesajlar.size() > 0 ? kisidenGelenBekleyenMesajlar.get(0).getId() : -1L));

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void kisiMesajPaneliKapandi(final String uuid) {

		islemKuyrugu.execute(() -> {

			model.mesajPaneliKapandi(uuid);

		});

	}

	@Override
	public void mesajGonderTiklandi(final String mesaj, final String aliciUuid) {

		islemKuyrugu.execute(() -> {

			try {

				final Message yeniMesaj = mesajGonder(gidenMesajOlustur(mesaj, aliciUuid));

				paneleMesajEkle(yeniMesaj);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void sayfaBasaKaydirildi(final String uuid) {

		islemKuyrugu.execute(() -> {

			Long oncekiMinMesajId = model.getMinMesajId(uuid);

			if (oncekiMinMesajId < 0)
				return;

			List<Message> iddenOncekiSonMesajlar = veritabaniYonetici.getIddenOncekiSonMesajlar(
					model.getKimlik().getUuid(), uuid, oncekiMinMesajId, SAYFA_MIN_MESAJ_SAYISI);

			if (iddenOncekiSonMesajlar.size() == 0)
				return;

			Platform.runLater(() -> mcPanel.konumuKaydet(uuid, oncekiMinMesajId));

			iddenOncekiSonMesajlar.forEach(mesaj -> paneleMesajEkle(mesaj));

			Platform.runLater(() -> mcPanel.kaydedilenKonumaGit(uuid));

		});

	}

	@Override
	public void grupOlusturTalepEdildi(String grupAdi, List<String> seciliUuidler) {

		Group grup = new Group(grupAdi, model.getKimlik().getUuid());

		seciliUuidler.forEach(uuid -> {

			try {

				Contact kisi = veritabaniYonetici.getKisi(uuid);

				if (kisi != null)
					grup.getKisiler().add(kisi);

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

		try {

			Group yeniGrup = veritabaniYonetici.grupEkleGuncelle(grup);

			model.addGrup(yeniGrup);

			Platform.runLater(() -> mcPanel.grupGuncelle(yeniGrup));

			// TODO

		} catch (HibernateException e) {

			e.printStackTrace();

		}

	}

}
