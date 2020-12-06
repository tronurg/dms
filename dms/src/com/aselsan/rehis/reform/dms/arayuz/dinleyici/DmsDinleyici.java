package com.aselsan.rehis.reform.dms.arayuz.dinleyici;

import java.nio.file.Path;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsDosya;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsGrup;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsKisi;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsListe;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsMesaj;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsNesne;

public interface DmsDinleyici {

	void dosyaTiklandi(Path dosyaYolu);

	void mesajAlindi(DmsMesaj dmsMesaj);

	void nesneAlindi(DmsNesne dmsNesne);

	void listeAlindi(DmsListe dmsListe);

	void dosyaAlindi(DmsDosya dmsDosya);

	void kisiGuncellendi(DmsKisi dmsKisi);

	void grupGuncellendi(DmsGrup dmsGrup);

}
