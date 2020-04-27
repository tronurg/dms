package com.aselsan.rehis.reform.mcsy.sunum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aselsan.rehis.reform.mcsy.sunum.intf.UygulamaDinleyici;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Grup;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;
import com.aselsan.rehis.reform.mcsy.veriyapilari.MesajYonu;

import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class McPanel extends StackPane implements IKimlikPane, IKisilerPane, IGruplarPane {

	private final VBox anaPane = new VBox();
	private final KimlikPane kimlikPane = new KimlikPane();
	private final KisilerPane kisilerPane = new KisilerPane();
	private final GruplarPane gruplarPane = new GruplarPane();
	private final VBox kisilerGruplarPane = new VBox();

	private final List<UygulamaDinleyici> dinleyiciler = Collections
			.synchronizedList(new ArrayList<UygulamaDinleyici>());

	public McPanel() {

		super();

		init();

	}

	private void init() {

		VBox.setMargin(kimlikPane, new Insets(10.0));

		VBox.setVgrow(kisilerPane, Priority.ALWAYS);
		VBox.setVgrow(gruplarPane, Priority.ALWAYS);
		VBox.setVgrow(kisilerGruplarPane, Priority.ALWAYS);

		kimlikPane.dinleyiciEkle(this);
		kisilerPane.dinleyiciEkle(this);
		gruplarPane.dinleyiciEkle(this);

		kisilerGruplarPane.getChildren().addAll(kisilerPane, gruplarPane);

		anaPane.getChildren().addAll(kimlikPane, kisilerGruplarPane);

		getChildren().add(anaPane);

	}

	public void dinleyiciEkle(UygulamaDinleyici dinleyici) {

		dinleyiciler.add(dinleyici);

	}

	public void setKimlik(Kimlik kimlik) {

		kimlikPane.setKimlik(kimlik);

	}

	public void kisiGuncelle(Kisi kisi) {

		kisilerPane.kisiGuncelle(kisi);
		gruplarPane.grupOlusturPaneKisiGuncelle(kisi);

	}

	public void grupGuncelle(Grup grup) {

		// TODO

		grup.getKisiler().forEach(kisi -> System.out.println(kisi.getIsim()));

	}

	public void mesajEkle(Mesaj mesaj, MesajYonu mesajYonu, String uuid) {

		kisilerPane.mesajEkle(mesaj, mesajYonu, uuid);

	}

	public void mesajGuncelle(Mesaj mesaj, String uuid) {

		kisilerPane.mesajGuncelle(mesaj, uuid);

	}

	public void ekraniMesajaKaydir(String uuid, Long mesajId) {

		kisilerPane.ekraniMesajaKaydir(uuid, mesajId);

	}

	public void konumuKaydet(String uuid, Long mesajId) {

		kisilerPane.konumuKaydet(uuid, mesajId);

	}

	public void kaydedilenKonumaGit(String uuid) {

		kisilerPane.kaydedilenKonumaGit(uuid);

	}

	private void dinleyicilereAciklamaGuncellendi(final String aciklama) {

		dinleyiciler.forEach(dinleyici -> dinleyici.aciklamaGuncellendi(aciklama));

	}

	private void dinleyicilereDurumGuncelleTiklandi() {

		dinleyiciler.forEach(dinleyici -> dinleyici.durumGuncelleTiklandi());

	}

	private void dinleyicilereKisiMesajPaneliAcildi(final String uuid) {

		dinleyiciler.forEach(dinleyici -> dinleyici.kisiMesajPaneliAcildi(uuid));

	}

	private void dinleyicilereKisiMesajPaneliKapandi(final String uuid) {

		dinleyiciler.forEach(dinleyici -> dinleyici.kisiMesajPaneliKapandi(uuid));

	}

	private void dinleyicilereMesajGonderTiklandi(final String mesaj, final String uuid) {

		dinleyiciler.forEach(dinleyici -> dinleyici.mesajGonderTiklandi(mesaj, uuid));

	}

	private void dinleyicilereSayfaBasaKaydirildi(final String uuid) {

		dinleyiciler.forEach(dinleyici -> dinleyici.sayfaBasaKaydirildi(uuid));

	}

	private void dinleyicilereGrupOlusturTalepEdildi(final String grupAdi, final List<String> seciliUuidler) {

		dinleyiciler.forEach(dinleyici -> dinleyici.grupOlusturTalepEdildi(grupAdi, seciliUuidler));

	}

	@Override
	public void aciklamaGuncellendi(String aciklama) {

		dinleyicilereAciklamaGuncellendi(aciklama);

	}

	@Override
	public void durumGuncelleTiklandi() {

		dinleyicilereDurumGuncelleTiklandi();

	}

	@Override
	public void mesajPaneGoster(final MesajPane mesajPane, final String uuid) {

		getChildren().add(mesajPane);

		dinleyicilereKisiMesajPaneliAcildi(uuid);

	}

	@Override
	public void mesajPaneGizle(MesajPane mesajPane, String uuid) {

		dinleyicilereKisiMesajPaneliKapandi(uuid);

		getChildren().remove(mesajPane);

	}

	@Override
	public void mesajGonderTiklandi(String mesajTxt, String uuid) {

		dinleyicilereMesajGonderTiklandi(mesajTxt, uuid);

	}

	@Override
	public void sayfaBasaKaydirildi(String uuid) {

		dinleyicilereSayfaBasaKaydirildi(uuid);

	}

	@Override
	public void grupOlusturPaneGoster(GrupOlusturPane grupOlusturPane) {

		getChildren().add(grupOlusturPane);

	}

	@Override
	public void grupOlusturPaneGizle(GrupOlusturPane grupOlusturPane) {

		getChildren().remove(grupOlusturPane);

	}

	@Override
	public void grupOlusturTiklandi(GrupOlusturPane grupOlusturPane) {

		getChildren().remove(grupOlusturPane);
		dinleyicilereGrupOlusturTalepEdildi(grupOlusturPane.getGrupAdi(), grupOlusturPane.getSeciliUuidler());
		grupOlusturPane.reset();

	}

	@Override
	public void grupMesajPaneGoster(MesajPane mesajPane, String uuid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void grupMesajPaneGizle(MesajPane mesajPane, String uuid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void grupMesajGonderTiklandi(String mesajTxt, String uuid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void grupSayfaBasaKaydirildi(String uuid) {
		// TODO Auto-generated method stub

	}

}
