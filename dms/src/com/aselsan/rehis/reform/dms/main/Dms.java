package com.aselsan.rehis.reform.dms.main;

import org.osgi.service.component.annotations.Component;

import com.aselsan.rehis.reform.dms.arayuz.DmsKontrol;
import com.aselsan.rehis.reform.dms.arayuz.DmsServis;
import com.aselsan.rehis.reform.dms.arayuz.hata.VeritabaniHatasi;
import com.aselsan.rehis.reform.dms.kontrol.Kontrol;

@Component
public class Dms implements DmsServis {

	@Override
	public DmsKontrol girisYap(String kullaniciAdi, String sifre) throws VeritabaniHatasi {

		return Kontrol.getInstance(kullaniciAdi, sifre);

	}

}
