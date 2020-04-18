package com.aselsan.rehis.reform.mcsy.main;

import org.osgi.service.component.annotations.Component;

import com.aselsan.rehis.reform.mcsy.arayuz.McHandle;
import com.aselsan.rehis.reform.mcsy.arayuz.McServisi;
import com.aselsan.rehis.reform.mcsy.arayuz.exceptions.VeritabaniHatasi;
import com.aselsan.rehis.reform.mcsy.kontrol.Kontrol;

@Component
public class Mcsy implements McServisi {

	@Override
	public McHandle login(String kullaniciAdi, String kullaniciSifresi) throws VeritabaniHatasi {

		return Kontrol.getInstance(kullaniciAdi, kullaniciSifresi);

	}

}
