package com.aselsan.rehis.reform.dms.arayuz.dinleyici;

import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsDosya;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsMesaj;

public interface DmsGuiDinleyici {

	void guiMesajGonderildi(DmsMesaj dmsMesaj);

	void guiMesajAlindi(DmsMesaj dmsMesaj);

	void guiDosyaGonderildi(DmsDosya dmsDosya);

	void guiDosyaAlindi(DmsDosya dmsDosya);

	void guiRaporGonderildi(DmsDosya dmsDosya);

	void guiRaporAlindi(DmsDosya dmsDosya);

	void guiSesKaydiGonderildi(DmsDosya dmsDosya);

	void guiSesKaydiAlindi(DmsDosya dmsDosya);

}
