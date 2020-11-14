package com.aselsan.rehis.reform.dms.arayuz.kontrol;

import java.nio.file.Path;

public interface DmsDosya {

	Integer getDosyaKodu();

	Path getDosyaYolu();

	Long getKisiId();

	Long getGrupId();

}
