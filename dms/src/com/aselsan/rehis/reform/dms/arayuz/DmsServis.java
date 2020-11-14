package com.aselsan.rehis.reform.dms.arayuz;

import com.aselsan.rehis.reform.dms.arayuz.hata.VeritabaniHatasi;

public interface DmsServis {

	DmsKontrol girisYap(String kullaniciAdi, String sifre) throws VeritabaniHatasi;

}
