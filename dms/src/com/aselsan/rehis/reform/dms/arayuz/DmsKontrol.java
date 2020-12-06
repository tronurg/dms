package com.aselsan.rehis.reform.dms.arayuz;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JComponent;

import com.aselsan.rehis.reform.dms.arayuz.dinleyici.DmsDinleyici;
import com.aselsan.rehis.reform.dms.arayuz.dinleyici.DmsGuiDinleyici;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsGrup;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsGrupSecim;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsKisi;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsKisiSecim;
import com.aselsan.rehis.reform.dms.arayuz.veriyapisi.DmsDurum;

public interface DmsKontrol {

	JComponent getDmsPanel();

	void dinleyiciEkle(DmsDinleyici dmsDinleyici);

	void dinleyiciCikar(DmsDinleyici dmsDinleyici);

	void guiDinleyiciEkle(DmsGuiDinleyici dmsGuiDinleyici);

	void guiDinleyiciCikar(DmsGuiDinleyici dmsGuiDinleyici);

	void setKoordinatlar(Double enlem, Double boylam);

	void setAciklama(String aciklama);

	void setDurum(DmsDurum dmsDurum);

	DmsKisi getDmsKisim();

	DmsGrupSecim getDmsAktifGruplarim();

	DmsKisiSecim getDmsCevrimiciKisiler();

	DmsKisi getDmsKisi(Long kisiId);

	DmsGrup getDmsGrup(Long grupId);

	List<DmsKisi> getTumDmsKisiler();

	List<DmsGrup> getTumDmsGruplar();

	List<Long> getAdrestekiIdler(InetAddress adres);

	boolean kisilereMesajGonder(String mesaj, Integer mesajKodu, List<Long> kisiIdler);

	boolean grubaMesajGonder(String mesaj, Integer mesajKodu, Long grupId);

	boolean kisilereNesneGonder(Object nesne, Integer nesneKodu, List<Long> kisiIdler);

	boolean grubaNesneGonder(Object nesne, Integer nesneKodu, Long grupId);

	<T> boolean kisilereListeGonder(List<T> liste, Class<T> elemanTipi, Integer listeKodu, List<Long> kisiIdler);

	<T> boolean grubaListeGonder(List<T> liste, Class<T> elemanTipi, Integer listeKodu, Long grupId);

	boolean kisilereDosyaGonder(Path dosyaYolu, Integer dosyaKodu, List<Long> kisiIdler);

	boolean grubaDosyaGonder(Path dosyaYolu, Integer dosyaKodu, Long grupId);

}
