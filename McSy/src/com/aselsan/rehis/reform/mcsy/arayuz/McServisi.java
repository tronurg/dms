package com.aselsan.rehis.reform.mcsy.arayuz;

import com.aselsan.rehis.reform.mcsy.arayuz.exceptions.VeritabaniHatasi;

public interface McServisi {

	McHandle login(String kullaniciAdi, String kullaniciSifresi) throws VeritabaniHatasi;

}
